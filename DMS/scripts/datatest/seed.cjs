/* DataTest local: SQL có marker, tài khoản Keycloak thật, mật khẩu ngẫu nhiên chỉ ghi file .local. */
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const dms = path.resolve(__dirname, '../..');
const args = process.argv.slice(2);
const value = (flag, fallback) => args.includes(flag) ? args[args.indexOf(flag) + 1] : fallback;
const output = path.resolve(value('--output', path.join(dms, 'backend/data/datatest')));
const container = value('--container', 'dms-postgres');
const identityContainer = value('--identity-db-container', 'dms-keycloak-postgres-1');
const marker = 'DMS-DATATEST-V1';
const env = Object.fromEntries(fs.readFileSync(path.join(dms, '.env'), 'utf8').split(/\r?\n/)
  .filter((line) => /^[A-Z_][A-Z0-9_]*=/.test(line)).map((line) => {
    const index = line.indexOf('=');
    return [line.slice(0, index), line.slice(index + 1).trim().replace(/^(["'])(.*)\1$/, '$2')];
  }));
const quote = (value) => "'" + String(value).replaceAll("'", "''") + "'";

function command(executable, argv, input) {
  const result = spawnSync(executable, argv, { input, encoding: 'utf8', windowsHide: true, maxBuffer: 8 * 1024 * 1024 });
  if (result.status !== 0) {
    // SQL không mang mật khẩu; không in command line hoặc nội dung môi trường.
    throw new Error(`${executable} thất bại: ${(result.stderr || result.error?.message || '').slice(-1600)}`);
  }
  return result.stdout;
}

function sql(statement) {
  return command('docker', ['exec', '-i', container, 'psql', '-X', '-U', env.DB_USERNAME || 'dms',
    '-d', env.DB_NAME || 'dms', '-v', 'ON_ERROR_STOP=1', '-q', '-t', '-A'], statement).trim();
}

function rows(statement) {
  return JSON.parse(sql(`SELECT COALESCE(jsonb_agg(to_jsonb(t)), '[]'::jsonb) FROM (${statement}) t;`));
}

function save(file, object) {
  const target = path.join(output, file);
  const temporary = target + '.tmp';
  fs.writeFileSync(temporary, JSON.stringify(object, null, 2), { mode: 0o600 });
  fs.renameSync(temporary, target);
}

function backup() {
  const folder = path.join(output, 'backups');
  fs.mkdirSync(folder, { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  command('docker', ['exec', container, 'pg_dump', '-U', env.DB_USERNAME || 'dms', '-d', env.DB_NAME || 'dms',
    '-Fc', '-f', '/tmp/dms-before-datatest.dump']);
  command('docker', ['cp', `${container}:/tmp/dms-before-datatest.dump`, path.join(folder, `dms-${stamp}.dump`)]);
  command('docker', ['exec', identityContainer, 'sh', '-c',
    'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/keycloak-before-datatest.dump']);
  command('docker', ['cp', `${identityContainer}:/tmp/keycloak-before-datatest.dump`, path.join(folder, `keycloak-${stamp}.dump`)]);
}

function prepareBase() {
  let source = fs.readFileSync(path.join(dms, 'scripts/sample-data.sql'), 'utf8');
  const originalUser = "'mau.sv.' || lpad(i::TEXT, 2, '0')";
  const originalCode = "'MAU-SV-' || lpad(i::TEXT, 2, '0')";
  if (!source.includes(originalUser) || !source.includes(originalCode)) {
    throw new Error('sample-data.sql đã đổi định dạng. Cần review bộ chuyển DataTest trước khi nạp.');
  }
  source = source.replaceAll(originalUser, "'990026' || lpad(i::TEXT, 4, '0')")
    .replaceAll(originalCode, "'990026' || lpad(i::TEXT, 4, '0')")
    .replaceAll('DMS-SAMPLE-V1', marker).replaceAll('dms.sample-data.v1', 'dms.datatest.v1')
    .replaceAll('mau.', 'dt.').replaceAll('MAU-', 'DT-').replaceAll('Mẫu -', 'DataTest -')
    .replaceAll('Dữ liệu mẫu', 'DataTest').replaceAll('Kho mẫu', 'Kho DataTest');
  source = source.replace("IF EXISTS (SELECT 1 FROM app_user WHERE username LIKE 'dt.%')",
    () => "IF EXISTS (SELECT 1 FROM app_user WHERE username LIKE 'dt.%' OR username ~ '^990026[0-9]{4}$')");
  fs.writeFileSync(path.join(output, 'base.sql'), source);
  return source;
}

async function provision() {
  const server = (env.KEYCLOAK_URL || 'http://localhost:8180').replace(/\/+$/, '');
  const realm = env.KEYCLOAK_REALM || 'dms';
  if (!['localhost', '127.0.0.1', '[::1]'].includes(new URL(server).hostname)) {
    throw new Error('Bộ DataTest chỉ cấp tài khoản trên Keycloak local.');
  }
  if (!env.DMS_KEYCLOAK_ADMIN_CLIENT_SECRET) throw new Error('Thiếu secret service account Keycloak trong DMS/.env');
  const manifestPath = path.join(output, 'accounts.local');
  const manifest = fs.existsSync(manifestPath) ? JSON.parse(fs.readFileSync(manifestPath, 'utf8')) :
    { marker, createdAt: new Date().toISOString(), loginUrl: 'http://localhost:5173/login', accounts: [] };
  if (manifest.marker !== marker) throw new Error('Manifest không thuộc bộ DataTest này.');
  let token;
  let tokenExpires = 0;
  async function request(route, options = {}) {
    if (Date.now() >= tokenExpires) {
      const response = await fetch(`${server}/realms/${realm}/protocol/openid-connect/token`, {
        method: 'POST', body: new URLSearchParams({ grant_type: 'client_credentials', client_id: 'dms-admin',
          client_secret: env.DMS_KEYCLOAK_ADMIN_CLIENT_SECRET }), signal: AbortSignal.timeout(15000),
      });
      if (!response.ok) throw new Error(`Không lấy được quyền quản trị Keycloak (${response.status})`);
      const data = await response.json();
      token = data.access_token;
      tokenExpires = Date.now() + (data.expires_in - 20) * 1000;
    }
    const response = await fetch(`${server}/admin/realms/${realm}${route}`, {
      ...options, headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      signal: AbortSignal.timeout(15000),
    });
    if (!response.ok) throw new Error(`Keycloak ${options.method || 'GET'} thất bại (${response.status}); chạy lại cùng manifest để tiếp tục.`);
    return response;
  }
  const users = rows("SELECT u.id,u.username,u.full_name,u.role,u.locked,u.must_change_password,u.security_version,u.keycloak_user_id,mu.code AS unit"
    + " FROM app_user u LEFT JOIN management_unit mu ON mu.id=u.management_unit_id"
    + " WHERE u.username IN ('dt.admin','dt.ql.1','dt.ql.2','dt.kt.1','dt.kt.2','dt.ql.tong-hop','dt.ql.khoa')"
    + " OR (u.username ~ '^990026[0-9]{4}$' AND u.username BETWEEN '9900260001' AND '9900260040') ORDER BY u.id");
  const roles = {};
  for (const role of [...new Set(users.map((user) => user.role))]) {
    roles[role] = await (await request('/roles/' + role)).json();
  }
  for (const user of users) {
    let entry = manifest.accounts.find((account) => account.username === user.username);
    if (user.keycloak_user_id) {
      if (!entry || entry.identityId !== user.keycloak_user_id) {
        throw new Error(`Tài khoản ${user.username} đã có liên kết ngoài manifest; không chiếm quyền hay reset mật khẩu.`);
      }
      continue;
    }
    const matches = await (await request('/users?exact=true&username=' + encodeURIComponent(user.username))).json();
    if (matches.length && !entry?.creating) throw new Error(`Tên ${user.username} đã có ở Keycloak; dừng để bảo toàn tài khoản.`);
    if (!entry) {
      entry = { id: user.id, username: user.username, role: user.role, unit: user.unit,
        password: 'Dt7!' + crypto.randomBytes(18).toString('base64url'), locked: user.locked,
        mustChangePassword: user.must_change_password, creating: true };
      manifest.accounts.push(entry);
      save('accounts.local', manifest);
    }
    let identityId = matches[0]?.id;
    if (!identityId) {
      const response = await request('/users', { method: 'POST', body: JSON.stringify({
        username: user.username, firstName: user.full_name, enabled: !user.locked,
        requiredActions: user.must_change_password ? ['UPDATE_PASSWORD'] : [],
        attributes: { dms_security_version: [String(user.security_version)] },
        credentials: [{ type: 'password', value: entry.password, temporary: user.must_change_password }],
      }) });
      identityId = response.headers.get('location')?.split('/').at(-1);
      if (!identityId) throw new Error('Keycloak không trả ID tài khoản đã tạo.');
    }
    entry.identityId = identityId;
    save('accounts.local', manifest);
    await request(`/users/${identityId}/role-mappings/realm`, { method: 'POST', body: JSON.stringify([roles[user.role]]) });
    sql(`UPDATE app_user SET keycloak_user_id=${quote(identityId)},password_hash=NULL WHERE id=${Number(user.id)} AND keycloak_user_id IS NULL;`);
    entry.creating = false;
    save('accounts.local', manifest);
  }
  return users.length;
}

async function main() {
  if (!args.includes('--apply')) {
    process.stdout.write('Dùng: node DMS/scripts/datatest/seed.cjs --apply [--output <thư mục local>]\n');
    return;
  }
  fs.mkdirSync(output, { recursive: true });
  backup();
  sql(prepareBase());
  sql(fs.readFileSync(path.join(__dirname, 'extra.sql'), 'utf8'));
  const accounts = await provision();
  const summary = rows("SELECT 'students' AS resource,count(*) AS total FROM student_profile WHERE student_code ~ '^990026[0-9]{4}$'"
    + " UNION ALL SELECT 'rooms',count(*) FROM room r JOIN building b ON b.id=r.building_id WHERE b.code LIKE 'DT-%'"
    + " UNION ALL SELECT 'contracts',count(*) FROM dormitory_contract WHERE code LIKE 'DT-%'"
    + " UNION ALL SELECT 'invoices',count(*) FROM invoice WHERE code LIKE 'DT-%'"
    + " UNION ALL SELECT 'tickets',count(*) FROM maintenance_request WHERE code LIKE 'DT-%'");
  save('summary.json', { marker, checkedAt: new Date().toISOString(), accounts, counts: summary });
  process.stdout.write(JSON.stringify({ accounts, counts: summary, credentialsSaved: true }) + '\n');
}

main().catch((error) => { process.stderr.write(error.message + '\n'); process.exitCode = 1; });
