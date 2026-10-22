/* Kiểm tra OIDC/UI/API thật trên local; không lưu token hay in mật khẩu ra log. */
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const dms = path.resolve(__dirname, '../..');
const args = process.argv.slice(2);
const output = path.resolve(args.includes('--output') ? args[args.indexOf('--output') + 1] : path.join(dms, 'backend/data/datatest'));
const base = process.env.DMS_TEST_URL || 'http://localhost:5173';
assert(['localhost', '127.0.0.1'].includes(new URL(base).hostname), 'Chỉ kiểm thử ứng dụng local');
const localEnv = Object.fromEntries(fs.readFileSync(path.join(dms, '.env'), 'utf8').split(/\r?\n/)
  .filter((line) => /^[A-Z_][A-Z0-9_]*=/.test(line)).map((line) => [line.slice(0, line.indexOf('=')), line.slice(line.indexOf('=') + 1).trim()]));
const identityUrl = (process.env.DMS_TEST_KEYCLOAK_URL || localEnv.KEYCLOAK_URL || 'http://localhost:8180').replace(/\/+$/, '');
const realm = localEnv.KEYCLOAK_REALM || 'dms';
const { chromium } = require(process.env.DMS_PLAYWRIGHT_MODULE || path.join(dms, 'backend/data/browser-check/node_modules/playwright'));
const manifest = JSON.parse(fs.readFileSync(path.join(output, 'accounts.local'), 'utf8'));
const secrets = new Set(manifest.accounts.map((account) => account.password));
const safe = (error) => {
  let message = String(error?.message || error);
  for (const secret of secrets) if (secret) message = message.split(secret).join('[REDACTED]');
  return message.replace(/Bearer\s+[A-Za-z0-9._-]+/g, 'Bearer [REDACTED]');
};
const results = { checkedAt: new Date().toISOString(), pages: [], checks: [], browserErrors: [] };
const previousFile = path.join(output, 'ui-results.json');
const previous = args.includes('--retry-failed') && fs.existsSync(previousFile)
  ? JSON.parse(fs.readFileSync(previousFile, 'utf8')) : null;
if (previous) results.pages = previous.pages.filter((item) => item.passed);
const images = path.join(output, 'screenshots');
fs.mkdirSync(images, { recursive: true });
function save(name, data) {
  fs.writeFileSync(path.join(output, name), JSON.stringify(data, null, 2), { mode: 0o600 });
}
function sql(statement) {
  const result = spawnSync('docker', ['exec', '-i', 'dms-postgres', 'psql', '-X', '-U', 'dms', '-d', 'dms', '-t', '-A', '-v', 'ON_ERROR_STOP=1'],
    { input: statement, encoding: 'utf8', windowsHide: true });
  assert.equal(result.status, 0, 'Đọc database DataTest thành công');
  return result.stdout.trim();
}
const fixture = JSON.parse(sql(`SELECT json_build_object(
  'unit', (SELECT id FROM management_unit WHERE code='K1'),
  'ownRoom',(SELECT r.id FROM room r JOIN building b ON b.id=r.building_id WHERE b.code='DT-1' AND r.code='M101'),
  'otherRoom',(SELECT r.id FROM room r JOIN building b ON b.id=r.building_id WHERE b.code='DT-2' AND r.code='M101'),
  'application',(SELECT a.id FROM room_application a JOIN student_profile p ON p.user_id=a.student_id WHERE p.student_code='9900260001' LIMIT 1),
  'contract',(SELECT id FROM dormitory_contract WHERE code='DT-HD-01'),
  'otherContract',(SELECT id FROM dormitory_contract WHERE code='DT-HD-02'),
  'ticket',(SELECT id FROM maintenance_request WHERE code='DT-TK-01'),
  'techTicket',(SELECT id FROM maintenance_request WHERE code='DT-TK-08'),
  'otherTechTicket',(SELECT id FROM maintenance_request WHERE code='DT-TK-02'),
  'transfer',(SELECT t.id FROM room_transfer_request t JOIN dormitory_contract c ON c.id=t.contract_id WHERE c.code='DT-HD-02' AND t.status='PENDING' LIMIT 1));`));

async function check(name, action) {
  try { await action(); results.checks.push({ name, passed: true }); }
  catch (error) { results.checks.push({ name, passed: false, error: safe(error) }); }
  save('ui-results.json', results);
}

async function main() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const contexts = [];
  let orphanId;
  let identity;
  let verificationUser;
  let admin;
  async function login(account) {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true });
    contexts.push(context);
    const page = await context.newPage();
    page.setDefaultTimeout(35000);
    let bearer;
    page.on('request', (request) => {
      if (request.url().startsWith(base + '/api/')) {
        const value = request.headers().authorization;
        if (value) { bearer = value; secrets.add(value); }
      }
    });
    page.on('pageerror', (error) => results.browserErrors.push(safe(error)));
    await page.goto(base + '/login', { waitUntil: 'networkidle' });
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await page.locator('#username').fill(account.username);
    secrets.add(account.password);
    await page.locator('#password').fill(account.password);
    await page.locator('#kc-login').click();
    await page.waitForFunction((origin) => Boolean(document.querySelector('#password-new')) ||
      (location.origin === origin && /^\/(admin|staff|student|technician)/.test(location.pathname)), base, { timeout: 60000 });
    if (await page.locator('#password-new').count()) {
      await page.screenshot({ path: path.join(images, 'keycloak-required-password-change.png'), fullPage: true });
      const password = 'Dt7!' + crypto.randomBytes(18).toString('base64url');
      secrets.add(password);
      await page.locator('#password-new').fill(password);
      await page.locator('#password-confirm').fill(password);
      await page.locator('input[type=submit],button[type=submit]').first().click();
      account.password = password;
      account.mustChangePassword = false;
      account.passwordChanged = true;
    }
    await page.waitForURL((url) => url.origin === base && /^\/(admin|staff|student|technician)/.test(url.pathname), { timeout: 60000 });
    await page.waitForLoadState('networkidle');
    assert(bearer, 'Trình duyệt phải dùng Bearer JWT từ luồng OIDC');
    return { context, page, bearer: () => bearer, account };
  }
  async function api(session, method, endpoint, data, expected = 200, extra = {}) {
    const response = await session.page.request.fetch(base + '/api' + endpoint, {
      method, headers: { Authorization: session.bearer() },
      ...(data === undefined ? {} : { data }), ...extra,
    });
    assert.equal(response.status(), expected, `${method} ${endpoint}: HTTP ${response.status()}`);
    return expected === 204 ? null : await response.json();
  }
  async function pageCheck(session, pathname, mobile = false) {
    if (previous?.pages.some((item) => item.path === pathname && Boolean(item.mobile) === mobile && item.passed)) return;
    const bad = [];
    const listener = (response) => {
      if (response.url().startsWith(base + '/api/') && response.status() >= 400) bad.push({ path: new URL(response.url()).pathname, status: response.status() });
    };
    session.page.on('response', listener);
    try {
      await session.page.setViewportSize(mobile ? { width: 360, height: 800 } : { width: 1440, height: 960 });
      await session.page.goto(base + pathname, { waitUntil: 'networkidle' });
      const finalPath = new URL(session.page.url()).pathname;
      const root = /^\/(admin|staff|student|technician)$/.test(pathname);
      assert(root ? finalPath.startsWith(pathname) : finalPath === pathname, `Guard đổi đường dẫn ${pathname} -> ${finalPath}`);
      assert((await session.page.locator('body').innerText()).length > 80, `Màn ${pathname} có nội dung`);
      assert.equal(bad.length, 0, `${pathname} API lỗi: ${JSON.stringify(bad)}`);
      const screenshot = `${pathname.replace(/[^a-z0-9]+/gi, '-').replace(/^-|-$/g, '') || 'home'}${mobile ? '-mobile' : ''}.png`;
      await session.page.screenshot({ path: path.join(images, screenshot), fullPage: true });
      results.pages.push({ path: pathname, mobile, passed: true, screenshot });
    } catch (error) {
      results.pages.push({ path: pathname, mobile, passed: false, error: safe(error), bad });
    } finally {
      session.page.off('response', listener);
      save('ui-results.json', results);
    }
  }
  try {
    admin = await login(manifest.accounts.find((account) => account.username === 'dt.admin'));
    await check('ADMIN đăng nhập OIDC và có 106 quyền', async () => {
      const me = await api(admin, 'GET', '/auth/me');
      assert.equal(me.role, 'ADMIN'); assert.equal(me.permissions.length, 106);
    });
    const anonymous = await admin.page.request.get(base + '/api/auth/me');
    await check('Chặn anonymous, lỗi có requestId', async () => { assert.equal(anonymous.status(), 401); assert((await anonymous.json()).requestId); });
    for (const role of ['ADMIN', 'STAFF', 'STUDENT', 'TECHNICIAN']) {
      await check('Đọc ma trận quyền ' + role, () => api(admin, 'GET', '/admin/role-permissions/' + role));
    }
    const staff = await login(manifest.accounts.find((account) => account.username === 'dt.ql.1'));
    const cross = await login(manifest.accounts.find((account) => account.username === 'dt.ql.tong-hop'));
    const student = await login(manifest.accounts.find((account) => account.username === '9900260001'));
    const studentTwo = await login(manifest.accounts.find((account) => account.username === '9900260002'));
    const technician = await login(manifest.accounts.find((account) => account.username === 'dt.kt.1'));
    await check('Staff đọc đúng đơn vị, ngoài đơn vị 404', async () => { await api(staff, 'GET', '/rooms/' + fixture.ownRoom); await api(staff, 'GET', '/rooms/' + fixture.otherRoom, undefined, 404); });
    await check('Staff tổng hợp đọc liên đơn vị, ghi ngoài đơn vị 404', async () => { await api(cross, 'GET', '/rooms/' + fixture.otherRoom); await api(cross, 'POST', '/rooms/save-or-update', { id: fixture.otherRoom, floor: 2 }, 404); });
    await check('Staff không được import hoặc xem audit', async () => { await api(staff, 'GET', '/admin/student-imports/template', undefined, 403); await api(staff, 'POST', '/admin/audit-logs/search-by-page', { page: 0, size: 10 }, 403); });
    await check('Sinh viên chỉ đọc hợp đồng của mình', async () => { await api(student, 'GET', '/contracts/' + fixture.contract); await api(student, 'GET', '/contracts/' + fixture.otherContract, undefined, 404); });
    await check('KTV chỉ đọc ticket được giao', async () => { await api(technician, 'GET', '/maintenance-requests/' + fixture.techTicket); await api(technician, 'GET', '/maintenance-requests/' + fixture.otherTechTicket, undefined, 404); });
    await check('Staff không đổi danh tính sinh viên', async () => { const me = await api(student, 'GET', '/auth/me'); await api(staff, 'POST', '/students/save-or-update', { id: me.id, fullName: 'DataTest - Không được sửa' }, 403); });
    await check('Hồ sơ cá nhân không cho sửa role/họ tên', () => api(student, 'POST', '/me/profile/save-or-update', { fullName: 'Không được sửa' }, 400));
    await check('Chi tiết audit có dữ liệu trước/sau và SYSTEM', async () => {
      const logs = await api(admin, 'POST', '/admin/audit-logs/search-by-page', { page: 0, size: 10, q: 'SYSTEM' });
      assert(logs.content.length); const detail = await api(admin, 'GET', '/admin/audit-logs/' + logs.content[0].id);
      assert.equal(detail.fullName, 'SYSTEM'); assert(Object.hasOwn(detail, 'newValue'));
    });

    const paths = (role) => [...fs.readFileSync(path.join(dms, `frontend/src/app/router/${role}Routes.tsx`), 'utf8').matchAll(/path:\s*'([^']+)'/g)]
      .map((match) => match[1]).filter((item) => !item.startsWith('/'));
    const staffPaths = paths('staff');
    const routePaths = { admin: [...new Set([...paths('admin'), ...staffPaths.filter((item) => item !== 'students')])],
      staff: staffPaths, student: paths('student'), technician: paths('technician') };
    const sessions = { admin, staff, student, technician };
    function resolveRoute(role, route) {
      const id = route.startsWith('rooms/') ? fixture.ownRoom : route.startsWith('applications/') ? fixture.application :
        route.startsWith('contracts/') ? fixture.contract : route.includes('transfers/') ? fixture.transfer :
          role === 'technician' ? fixture.techTicket : fixture.ticket;
      return '/' + role + '/' + route.replace(':id', String(id));
    }
    for (const [role, routes] of Object.entries(routePaths)) {
      await pageCheck(sessions[role], '/' + role);
      for (const route of routes) await pageCheck(role === 'student' && route.includes('transfers/') ? studentTwo : sessions[role], resolveRoute(role, route));
    }
    for (const pathname of ['/student', '/student/room', '/student/profile', '/technician/tasks', '/technician/profile']) {
      await pageCheck(pathname.startsWith('/student') ? student : technician, pathname, true);
    }
    save('screen-routes.json', routePaths);

    if (args.includes('--exercise-import')) {
      await check('Lookup đơn hợp đồng dùng POST với status trong body', async () => {
        await admin.page.goto(base + '/admin/contracts', { waitUntil: 'networkidle' });
        const pending = admin.page.waitForResponse((response) => response.url().endsWith('/applications/search-by-page') && response.request().method() === 'POST');
        await admin.page.getByRole('button', { name: /Thêm mới/ }).click();
        const response = await pending;
        assert.equal(response.status(), 200);
        assert.equal(response.request().postDataJSON().status, 'APPROVED');
        assert((await response.json()).content.every((row) => row.status === 'APPROVED'));
        await admin.page.getByRole('dialog').getByRole('button', { name: 'Đóng', exact: true }).last().click();
      });
      const folder = path.join(output, 'outputs/dms-datatest-20261021');
      const studentCodes = JSON.parse(fs.readFileSync(path.join(folder, 'expected.json'), 'utf8'))['import-hop-le.xlsx'].studentCodes;
      assert(studentCodes?.length === 8 && studentCodes.every((code) => /^[0-9]{6,20}$/.test(code)));
      const file = fs.readFileSync(path.join(folder, 'import-hop-le.xlsx'));
      const multipart = { file: { name: 'students.xlsx', mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', buffer: file } };
      const preview = await api(admin, 'POST', '/admin/student-imports?dryRun=true', undefined, 200, { multipart });
      assert.equal(preview.totalRows, 8); assert.equal(preview.errorRows, 0);
      if (preview.validRows === 8) {
        const env = localEnv;
        secrets.add(env.DMS_KEYCLOAK_ADMIN_CLIENT_SECRET);
        const tokenResponse = await fetch(`${identityUrl}/realms/${realm}/protocol/openid-connect/token`, { method: 'POST', body: new URLSearchParams({ grant_type: 'client_credentials', client_id: 'dms-admin', client_secret: env.DMS_KEYCLOAK_ADMIN_CLIENT_SECRET }), signal: AbortSignal.timeout(15000) });
        assert(tokenResponse.ok); const token = (await tokenResponse.json()).access_token; secrets.add(token);
        identity = async (route, options = {}) => { const response = await fetch(`${identityUrl}/admin/realms/${realm}` + route, { ...options, headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json' }, signal: AbortSignal.timeout(15000) }); assert(response.ok, 'Keycloak Admin REST HTTP ' + response.status); return response; };
        const username = studentCodes.at(-1);
        assert.equal((await (await identity('/users?exact=true&username=' + username)).json()).length, 0, 'Không chiếm tài khoản đã có');
        const orphan = await identity('/users', { method: 'POST', body: JSON.stringify({ username, firstName: 'DataTest collision', enabled: false }) });
        orphanId = orphan.headers.get('location').split('/').at(-1);
        await check('Import lỗi Keycloak rollback DMS và dọn mọi danh tính vừa tạo', async () => {
          await api(admin, 'POST', '/admin/student-imports?dryRun=false', undefined, 409, { multipart });
          assert.equal(Number(sql(`SELECT count(*) FROM app_user WHERE username IN (${studentCodes.map((code) => "'" + code + "'").join(',')});`)), 0);
          for (const code of studentCodes.slice(0, -1)) assert.equal((await (await identity('/users?exact=true&username=' + code)).json()).length, 0);
        });
        await identity('/users/' + orphanId, { method: 'DELETE' }); orphanId = undefined;
        await admin.page.goto(base + '/admin/import', { waitUntil: 'networkidle' });
        await admin.page.locator('input[type=file]').setInputFiles(path.join(folder, 'import-hop-le.xlsx'));
        let responsePromise = admin.page.waitForResponse((response) => response.url().includes('/student-imports?dryRun=true') && response.request().method() === 'POST');
        await admin.page.getByRole('button', { name: 'Kiểm tra dữ liệu', exact: true }).click();
        assert.equal((await (await responsePromise).json()).validRows, 8);
        await admin.page.getByRole('button', { name: 'Chỉ import dòng hợp lệ', exact: true }).click();
        responsePromise = admin.page.waitForResponse((response) => response.url().includes('/student-imports?dryRun=false') && response.request().method() === 'POST');
        await admin.page.getByRole('dialog').getByRole('button', { name: 'Chỉ import dòng hợp lệ', exact: true }).click();
        const written = await responsePromise; assert.equal(written.status(), 200);
        assert(written.headers()['cache-control'].includes('no-store'));
        const data = await written.json(); assert.equal(data.createdRows, 8);
        const imported = data.createdAccounts.map((account) => ({ id: account.id, username: account.username, password: account.temporaryPassword, role: 'STUDENT', mustChangePassword: true }));
        const credentialName = `import-${studentCodes[0]}.local`;
        imported.forEach((account) => secrets.add(account.password)); save(credentialName, imported);
        await admin.page.getByRole('dialog').waitFor({ state: 'hidden', timeout: 10000 });
        await admin.page.screenshot({ path: path.join(images, 'admin-import-success.png'), fullPage: true });
        const [downloaded] = await Promise.all([
          admin.page.waitForEvent('download', { timeout: 15000 }),
          admin.page.getByRole('button', { name: /Tải tài khoản vừa tạo/ }).click({ timeout: 15000, noWaitAfter: true }),
        ]);
        await Promise.race([downloaded.saveAs(path.join(output, `import-${studentCodes[0]}.csv.local`)),
          new Promise((_, reject) => setTimeout(() => reject(new Error('Tải CSV không hoàn tất')), 15000))]);
        const firstLogin = await login(imported[0]); save(credentialName, imported);
        assert(imported[0].passwordChanged, 'Keycloak bắt đổi mật khẩu trước khi cấp token');
        await api(firstLogin, 'GET', '/auth/me');
        results.checks.push({ name: 'UI import 8 tài khoản, tải mật khẩu một lần và bắt đổi mật khẩu đầu tiên', passed: true });
      }
      await check('Excel hỗn hợp trả đúng 1 hợp lệ / 1 có sẵn / 6 lỗi', async () => {
        await admin.page.goto(base + '/admin/import', { waitUntil: 'networkidle' });
        await admin.page.locator('input[type=file]').setInputFiles(path.join(folder, 'import-hon-hop.xlsx'));
        const pending = admin.page.waitForResponse((response) => response.url().includes('/student-imports?dryRun=true') && response.request().method() === 'POST');
        await admin.page.getByRole('button', { name: 'Kiểm tra dữ liệu', exact: true }).click();
        const result = await (await pending).json();
        assert.deepEqual([result.validRows, result.existingRows, result.errorRows], [1, 1, 6]);
        await admin.page.screenshot({ path: path.join(images, 'admin-import-mixed.png'), fullPage: true });
      });

      await check('Tài khoản thật: đổi role, khóa/mở, reset/logout đều thu hồi token cũ', async () => {
        const username = 'verify.dms.' + Date.now().toString(36);
        const created = await api(admin, 'POST', '/admin/users/save-or-update', { username, fullName: 'DataTest - Kiểm tra bảo mật', role: 'STAFF', managementUnitId: fixture.unit });
        verificationUser = { id: created.userId, username, password: created.temporaryPassword, role: 'STAFF' };
        const stored = await api(admin, 'GET', '/admin/users/' + created.userId); verificationUser.username = stored.username;
        secrets.add(created.temporaryPassword); save('verification-account.local', verificationUser);
        const before = await login(verificationUser); save('verification-account.local', verificationUser);
        await api(admin, 'POST', '/admin/users/save-or-update', { id: created.userId, role: 'TECHNICIAN', managementUnitId: fixture.unit });
        await api(before, 'GET', '/auth/me', undefined, 401);
        verificationUser.role = 'TECHNICIAN';
        const changed = await login(verificationUser);
        assert.equal((await api(changed, 'GET', '/auth/me')).role, 'TECHNICIAN');
        await api(admin, 'POST', `/admin/users/${created.userId}/lock`, { reason: 'DataTest - Kiểm tra khóa tài khoản' }, 204);
        await api(changed, 'GET', '/auth/me', undefined, 403);
        await api(admin, 'POST', `/admin/users/${created.userId}/unlock`, undefined, 204);
        await api(changed, 'GET', '/auth/me', undefined, 401);
        const reset = await api(admin, 'POST', `/admin/users/${created.userId}/reset-password`);
        verificationUser.password = reset.temporaryPassword; secrets.add(reset.temporaryPassword); save('verification-account.local', verificationUser);
        const resetLogin = await login(verificationUser); save('verification-account.local', verificationUser);
        await api(resetLogin, 'POST', '/auth/logout', undefined, 204);
        await api(resetLogin, 'GET', '/auth/me', undefined, 401);
      });
    }
  } finally {
    if (orphanId && identity) await identity('/users/' + orphanId, { method: 'DELETE' }).catch(() => {});
    if (verificationUser?.id && admin) {
      await api(admin, 'POST', `/admin/users/${verificationUser.id}/lock`, { reason: 'DataTest - Kết thúc kiểm tra, khóa tài khoản' }, 204).catch(() => {});
      verificationUser.locked = true; save('verification-account.local', verificationUser);
    }
    results.completedAt = new Date().toISOString();
    save('ui-results.json', results);
    await Promise.race([browser.close(), new Promise((resolve) => setTimeout(resolve, 5000))]);
  }
  const failures = results.pages.filter((item) => !item.passed).length + results.checks.filter((item) => !item.passed).length + results.browserErrors.length;
  process.stdout.write(JSON.stringify({ pages: results.pages.length, checks: results.checks.length, browserErrors: results.browserErrors.length, failures }) + '\n');
  if (failures) process.exitCode = 1;
}
main().then(() => process.exit(process.exitCode || 0)).catch((error) => {
  results.fatal = safe(error); save('ui-results.json', results); process.stderr.write(safe(error) + '\n'); process.exit(1);
});
