'use strict';
/**
 * Sinh bộ dữ liệu vận hành đầy đủ cho DMS (PostgreSQL, schema Flyway V19).
 *
 * Dùng: node gen-full-data.cjs <thư-mục-đầu-ra>
 * Kết quả: <thư-mục-đầu-ra>/full-data.sql. Chạy bằng psql -v ON_ERROR_STOP=1 -f full-data.sql.
 *
 * Script xóa toàn bộ dữ liệu nghiệp vụ rồi nạp lại; giữ nguyên role_permission_matrix,
 * system_config và tài khoản admin đã liên kết Keycloak. Hạt giống cố định nên chạy lại ra đúng dữ liệu cũ.
 * Mốc thời gian hiện tại là 2026-11-22 (NOW_DATE).
 */
const fs = require('fs');
const path = require('path');

// ---------------------------------------------------------------- tiện ích
function mulberry32(seed) {
  return function next() {
    seed |= 0;
    seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const R = mulberry32(20261122);
const rnd = (n) => Math.floor(R() * n);
const between = (a, b) => a + rnd(b - a + 1);
const pick = (a) => a[rnd(a.length)];
const chance = (p) => R() < p;
function shuffle(a) {
  const x = a.slice();
  for (let i = x.length - 1; i > 0; i--) {
    const j = rnd(i + 1);
    [x[i], x[j]] = [x[j], x[i]];
  }
  return x;
}
function wpick(pairs) {
  const total = pairs.reduce((s, p) => s + p[1], 0);
  let r = R() * total;
  for (const [value, weight] of pairs) {
    r -= weight;
    if (r < 0) return value;
  }
  return pairs[pairs.length - 1][0];
}

const NOW_DATE = '2026-11-22';
const d2n = (s) => Date.UTC(+s.slice(0, 4), +s.slice(5, 7) - 1, +s.slice(8, 10)) / 86400000;
const n2d = (n) => new Date(n * 86400000).toISOString().slice(0, 10);
const addDays = (s, n) => n2d(d2n(s) + n);
const diffDays = (a, b) => d2n(a) - d2n(b);
const pad = (n, w = 2) => String(n).padStart(w, '0');
const ts = (date, h, m) =>
  `${date} ${pad(h === undefined ? between(8, 17) : h)}:${pad(m === undefined ? between(0, 59) : m)}:${pad(between(0, 59))}+07`;
const tsDate = (t) => t.slice(0, 10);
const firstOfMonth = (date) => `${date.slice(0, 7)}-01`;

class Raw {
  constructor(s) {
    this.s = s;
  }
}
class J {
  constructor(v) {
    this.v = v;
  }
}
function lit(v) {
  if (v === null || v === undefined) return 'NULL';
  if (v instanceof Raw) return v.s;
  if (v instanceof J) return `'${JSON.stringify(v.v).replace(/'/g, "''")}'::jsonb`;
  if (typeof v === 'number') return String(v);
  if (typeof v === 'boolean') return v ? 'TRUE' : 'FALSE';
  return `'${String(v).replace(/'/g, "''")}'`;
}

const T = {};
const IDS = {};
function nid(t) {
  IDS[t] = (IDS[t] || 0) + 1;
  return IDS[t];
}
function add(t, row, hasId = true) {
  if (hasId) {
    if (row.id === undefined) row.id = nid(t);
    else IDS[t] = Math.max(IDS[t] || 0, row.id);
  }
  (T[t] = T[t] || []).push(row);
  return row;
}

// ---------------------------------------------------------------- dữ liệu nguồn
const HO = ['Nguyễn', 'Nguyễn', 'Nguyễn', 'Trần', 'Trần', 'Lê', 'Lê', 'Phạm', 'Phạm', 'Hoàng', 'Phan', 'Vũ', 'Võ', 'Đặng', 'Bùi', 'Đỗ', 'Hồ', 'Ngô', 'Dương', 'Lý', 'Đinh', 'Trịnh', 'Lương', 'Mai', 'Tạ', 'Cao', 'Chu', 'Kiều', 'Lưu', 'Đào'];
const MID_M = ['Văn', 'Đức', 'Minh', 'Quang', 'Hữu', 'Anh', 'Tuấn', 'Gia', 'Công', 'Xuân', 'Thành', 'Quốc', 'Đình', 'Ngọc', 'Việt', 'Bá', 'Trung', 'Hải'];
const MID_F = ['Thị', 'Thị', 'Ngọc', 'Thu', 'Phương', 'Khánh', 'Hồng', 'Minh', 'Mai', 'Thùy', 'Diệu', 'Bảo', 'Lan', 'Hải', 'Quỳnh', 'Kim', 'Tuyết', 'Thanh'];
const FIRST_M = ['An', 'Bình', 'Cường', 'Dũng', 'Đạt', 'Hải', 'Hiếu', 'Hoàng', 'Huy', 'Khánh', 'Long', 'Minh', 'Nam', 'Phúc', 'Quân', 'Sơn', 'Thắng', 'Tùng', 'Trung', 'Việt', 'Hưng', 'Kiên', 'Lâm', 'Đức', 'Dương', 'Tài', 'Toàn', 'Thịnh', 'Vinh', 'Bảo', 'Nghĩa', 'Khoa', 'Tuấn', 'Hà', 'Phong', 'Tiến', 'Đông'];
const FIRST_F = ['An', 'Anh', 'Chi', 'Dung', 'Giang', 'Hà', 'Hạnh', 'Hoa', 'Hương', 'Lan', 'Linh', 'Mai', 'My', 'Ngân', 'Nhung', 'Oanh', 'Phương', 'Quyên', 'Thảo', 'Thu', 'Trang', 'Trinh', 'Uyên', 'Vân', 'Yến', 'Hiền', 'Hằng', 'Ly', 'Nga', 'Ngọc', 'Nhi', 'Thủy', 'Tâm', 'Diệp', 'Khánh', 'Hồng'];
const usedNames = new Set();
function personName(gender) {
  for (;;) {
    const name = `${pick(HO)} ${gender === 'M' ? pick(MID_M) : pick(MID_F)} ${gender === 'M' ? pick(FIRST_M) : pick(FIRST_F)}`;
    if (!usedNames.has(name)) {
      usedNames.add(name);
      return name;
    }
  }
}
const PHONE_PREFIX = ['090', '091', '093', '094', '096', '097', '098', '032', '033', '034', '035', '036', '037', '038', '039', '070', '076', '077', '078', '079', '081', '082', '083', '084', '085', '086', '088', '089'];
const vnd = (n) => String(n).replace(/B(?=(d{3})+(?!d))/g, '.');
const phone = () => pick(PHONE_PREFIX) + String(between(0, 9999999)).padStart(7, '0');

// ---------------------------------------------------------------- danh mục dùng chung
const AY = [
  ['2024-2025', '2024-08-26', '2025-08-24'],
  ['2025-2026', '2025-08-25', '2026-08-23'],
  ['2026-2027', '2026-08-24', '2027-08-23'],
  ['2027-2028', '2027-08-24', '2028-08-21'],
];
AY.forEach(([code, start, end]) =>
  add('academic_year', {
    code,
    name: `Năm học ${code}`,
    active: code === '2026-2027',
    start_date: start,
    end_date: end,
    note: code === '2026-2027' ? 'Năm học hiện hành' : null,
  }),
);

const SCHOOLS = [
  ['TLU', 'Trường Đại học Thủy lợi', 'ĐH Thủy lợi', 'TLA', 'Số 175 Tây Sơn, Đống Đa, Hà Nội', true, 'Trường chủ quản ký túc xá'],
  ['NEU', 'Trường Đại học Kinh tế Quốc dân', 'ĐH Kinh tế Quốc dân', 'KHA', 'Số 207 Giải Phóng, Hai Bà Trưng, Hà Nội', false, 'Sinh viên liên kết ở ghép theo thỏa thuận'],
  ['VNUA', 'Học viện Nông nghiệp Việt Nam', 'HV Nông nghiệp', 'HVN', 'Trâu Quỳ, Gia Lâm, Hà Nội', false, null],
  ['HAUI', 'Trường Đại học Công nghiệp Hà Nội', 'ĐH Công nghiệp HN', 'DCN', 'Số 298 Cầu Diễn, Bắc Từ Liêm, Hà Nội', false, null],
];
SCHOOLS.forEach(([code, name, short, moet, address, home, note]) =>
  add('school', { code, name, active: true, short_name: short, moet_code: moet, address, is_home_school: home, note }),
);

const FACULTIES = [
  [1, 'CNTT', 'Khoa Công nghệ thông tin', 'CNTT', '02438522201', 'cntt@tlu.edu.vn'],
  [1, 'KTTNN', 'Khoa Kỹ thuật tài nguyên nước', 'KT TNN', '02438522202', 'kttnn@tlu.edu.vn'],
  [1, 'CT', 'Khoa Công trình', 'Công trình', '02438522203', 'congtrinh@tlu.edu.vn'],
  [1, 'CK', 'Khoa Cơ khí', 'Cơ khí', '02438522204', 'cokhi@tlu.edu.vn'],
  [1, 'DDT', 'Khoa Điện - Điện tử', 'Điện - Điện tử', '02438522205', 'dien@tlu.edu.vn'],
  [1, 'KTQL', 'Khoa Kinh tế và Quản lý', 'Kinh tế QL', '02438522206', 'kinhte@tlu.edu.vn'],
  [1, 'HMT', 'Khoa Hóa và Môi trường', 'Hóa - MT', '02438522207', 'hoamoitruong@tlu.edu.vn'],
  [1, 'LLCT', 'Khoa Luật và Lý luận chính trị', 'Luật - LLCT', '02438522208', 'luat@tlu.edu.vn'],
  [2, 'NEU-QTKD', 'Khoa Quản trị kinh doanh', 'QTKD', null, null],
  [3, 'VNUA-CNTT', 'Khoa Công nghệ thông tin', 'CNTT', null, null],
  [4, 'HAUI-CK', 'Khoa Cơ khí', 'Cơ khí', null, null],
];
FACULTIES.forEach(([school, code, name, short, tel, mail]) =>
  add('faculty', { code, name, active: true, school_id: school, short_name: short, phone: tel, email: mail, note: null }),
);
const TLU_FACULTY_IDS = [1, 2, 3, 4, 5, 6, 7, 8];
const FACULTY_WEIGHT = [[1, 22], [2, 14], [3, 14], [4, 10], [5, 12], [6, 16], [7, 8], [8, 4]];

const COHORTS = [
  ['K64', 2022, 2027],
  ['K65', 2023, 2028],
  ['K66', 2024, 2029],
  ['K67', 2025, 2030],
  ['K68', 2026, 2031],
];
COHORTS.forEach(([name, enroll, grad]) =>
  add('student_cohort', { code: name, name, active: true, enrollment_year: enroll, expected_graduation_year: grad, note: null }),
);

// [code, tên, gso, khu vực, vùng khó khăn]
const PROVINCES = [
  ['HN', 'Hà Nội', '01', 'KV3', false], ['HP', 'Hải Phòng', '31', 'KV3', false], ['QN', 'Quảng Ninh', '22', 'KV2', false],
  ['BN', 'Bắc Ninh', '27', 'KV2', false], ['HY', 'Hưng Yên', '33', 'KV2NT', false], ['HD', 'Hải Dương', '30', 'KV2NT', false],
  ['TB', 'Thái Bình', '34', 'KV2NT', false], ['ND', 'Nam Định', '36', 'KV2NT', false], ['HNA', 'Hà Nam', '35', 'KV2NT', false],
  ['NB', 'Ninh Bình', '37', 'KV2NT', false], ['TH', 'Thanh Hóa', '38', 'KV2NT', false], ['NA', 'Nghệ An', '40', 'KV2NT', false],
  ['HT', 'Hà Tĩnh', '42', 'KV2NT', false], ['QB', 'Quảng Bình', '44', 'KV2NT', false], ['VP', 'Vĩnh Phúc', '26', 'KV2NT', false],
  ['PT', 'Phú Thọ', '25', 'KV2NT', false], ['TN', 'Thái Nguyên', '19', 'KV2', false], ['BG', 'Bắc Giang', '24', 'KV2NT', false],
  ['LS', 'Lạng Sơn', '20', 'KV1', true], ['CB', 'Cao Bằng', '04', 'KV1', true], ['HG', 'Hà Giang', '02', 'KV1', true],
  ['LCA', 'Lào Cai', '10', 'KV1', true], ['YB', 'Yên Bái', '15', 'KV1', true], ['SL', 'Sơn La', '14', 'KV1', true],
  ['DB', 'Điện Biên', '11', 'KV1', true], ['LCH', 'Lai Châu', '12', 'KV1', true], ['HB', 'Hòa Bình', '17', 'KV1', true],
  ['TQ', 'Tuyên Quang', '08', 'KV1', true], ['BK', 'Bắc Kạn', '06', 'KV1', true], ['DNA', 'Đà Nẵng', '48', 'KV3', false],
  ['HCM', 'TP. Hồ Chí Minh', '79', 'KV3', false], ['DLK', 'Đắk Lắk', '66', 'KV1', false],
];
PROVINCES.forEach(([code, name, gso, area, hard]) =>
  add('hometown', { code, name, active: true, parent_id: null, admin_level: 'PROVINCE', gso_code: gso, priority_area: area, hardship_area: hard, note: null }),
);
const PROV_ID = Object.fromEntries(PROVINCES.map((p, i) => [p[0], i + 1]));
const COMMUNES = [
  ['HG', 'Xã Lũng Cú'], ['HG', 'Xã Thượng Phùng'], ['CB', 'Xã Thượng Hà'], ['DB', 'Xã Mường Nhé'], ['SL', 'Xã Chiềng Khoong'],
  ['LCH', 'Xã Dào San'], ['LCA', 'Xã Bản Xèo'], ['TH', 'Xã Pù Nhi'], ['NA', 'Xã Mường Lống'],
];
COMMUNES.forEach(([prov, name]) =>
  add('hometown', {
    code: `${prov}-${name.replace(/^Xã /, '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').replace(/\s+/g, '').toUpperCase()}`,
    name,
    active: true,
    parent_id: PROV_ID[prov],
    admin_level: 'COMMUNE',
    gso_code: null,
    priority_area: 'KV1',
    hardship_area: true,
    note: 'Xã đặc biệt khó khăn theo danh mục hiện hành',
  }),
);
const HARD_HOMETOWN_IDS = T.hometown.filter((h) => h.hardship_area).map((h) => h.id);
const NORMAL_HOMETOWN = T.hometown.filter((h) => !h.hardship_area).map((h) => h.id);
const HOMETOWN_WEIGHT_PROVINCE = ['HN', 'HP', 'QN', 'BN', 'HY', 'HD', 'TB', 'ND', 'HNA', 'NB', 'TH', 'NA', 'HT', 'VP', 'PT', 'BG', 'TN'];

const AMENITIES = [
  ['AC', 'Điều hòa nhiệt độ', 'ROOM', 'snowflake', 'Điều hòa hai chiều'],
  ['FAN', 'Quạt trần', 'ROOM', 'fan', 'Quạt trần có điều tốc'],
  ['HEATER', 'Bình nóng lạnh', 'ROOM', 'flame', 'Bình nóng lạnh 20 lít trong khu vệ sinh'],
  ['BUNK', 'Giường tầng sắt', 'ROOM', 'bed', 'Giường tầng khung sắt, kèm đệm'],
  ['WARDROBE', 'Tủ cá nhân', 'ROOM', 'archive', 'Mỗi sinh viên một ngăn tủ khóa riêng'],
  ['DESK', 'Bàn học', 'ROOM', 'book', 'Bàn học dài dùng chung'],
  ['WIFI', 'Wi-Fi tốc độ cao', 'BOTH', 'wifi', 'Phủ sóng toàn khu, đăng nhập bằng mã sinh viên'],
  ['BALCONY', 'Ban công', 'ROOM', 'sun', 'Ban công riêng có chỗ phơi đồ'],
  ['KITCHEN', 'Khu bếp riêng', 'ROOM', 'utensils', 'Bếp từ và tủ lạnh mini'],
  ['LAUNDRY', 'Khu giặt phơi chung', 'BUILDING', 'shirt', 'Có máy giặt công cộng tính phí theo lượt'],
  ['LIFT', 'Thang máy', 'BUILDING', 'arrow-up', 'Thang máy hoạt động 05:00–23:00'],
  ['CAMERA', 'Camera an ninh', 'BUILDING', 'camera', 'Camera hành lang và sảnh, lưu 30 ngày'],
  ['WATER_PURIFIER', 'Máy lọc nước nóng lạnh', 'BUILDING', 'droplet', 'Đặt tại sảnh mỗi tầng'],
  ['BIKE', 'Nhà để xe', 'BUILDING', 'bike', 'Nhà để xe có mái che, trông giữ theo thẻ'],
  ['COMMON_ROOM', 'Phòng sinh hoạt chung', 'BUILDING', 'users', 'Phòng học nhóm và sinh hoạt câu lạc bộ'],
];
AMENITIES.forEach(([code, name, applies, icon, desc]) =>
  add('amenity', { code, name, active: true, applies_to: applies, icon, description: desc, note: null }),
);
const AM = Object.fromEntries(AMENITIES.map((a, i) => [a[0], i + 1]));

// [code, tên, mức độ mặc định, SLA giờ, kỹ năng, mô tả]
const MAINT_CATS = [
  ['ELECTRIC', 'Điện', 'HIGH', 24, 'ELECTRIC', 'Hỏng ổ cắm, bóng đèn, quạt, cầu dao, đường dây'],
  ['WATER', 'Nước', 'MEDIUM', 48, 'PLUMBING', 'Rò rỉ, tắc nghẽn, mất nước, hỏng vòi và bình nóng lạnh'],
  ['FURNITURE', 'Nội thất', 'LOW', 96, 'CARPENTRY', 'Giường, tủ, bàn học, cửa sổ, rèm'],
  ['DOOR_LOCK', 'Cửa/khóa', 'MEDIUM', 24, 'GENERAL', 'Khóa cửa, bản lề, thẻ từ, chốt cửa'],
  ['OTHER', 'Khác', 'LOW', 120, 'GENERAL', 'Sự cố khác không thuộc các nhóm trên'],
];
MAINT_CATS.forEach(([code, name, sev, sla, skill, desc]) =>
  add('maintenance_category', { code, name, active: true, default_severity: sev, sla_hours: sla, skill, description: desc, note: null }),
);

// [code, tên, trọng số, bằng chứng, bắt buộc, căn cứ, thứ tự, auto_rule]
const PRIORITIES = [
  ['LIET_SI_TB', 'Con liệt sĩ, thương binh, bệnh binh', 30, 'Giấy xác nhận của Phòng Lao động - Thương binh và Xã hội', true, 'Pháp lệnh ưu đãi người có công với cách mạng', 1, 'NONE'],
  ['NGUOI_CO_CONG', 'Con người có công với cách mạng', 25, 'Quyết định hoặc giấy xác nhận người có công', true, 'Pháp lệnh ưu đãi người có công với cách mạng', 2, 'NONE'],
  ['KHUYET_TAT', 'Sinh viên khuyết tật', 25, 'Giấy xác nhận khuyết tật do UBND cấp xã cấp', true, 'Luật Người khuyết tật', 3, 'NONE'],
  ['HO_NGHEO', 'Hộ nghèo, hộ cận nghèo', 20, 'Giấy chứng nhận hộ nghèo/cận nghèo còn hiệu lực', true, 'Quy chế ký túc xá của Nhà trường', 4, 'NONE'],
  ['MO_COI', 'Mồ côi cha hoặc mẹ', 20, 'Giấy chứng tử hoặc xác nhận của địa phương', true, 'Quy chế ký túc xá của Nhà trường', 5, 'NONE'],
  ['DAN_TOC_THIEU_SO', 'Sinh viên dân tộc thiểu số', 15, 'Hồ sơ sinh viên ghi dân tộc', false, 'Quy chế ký túc xá của Nhà trường', 6, 'ETHNIC_MINORITY'],
  ['VUNG_KHO_KHAN', 'Quê quán vùng đặc biệt khó khăn', 15, 'Hệ thống tự đối chiếu theo danh mục quê quán', false, 'Danh mục địa bàn đặc biệt khó khăn hiện hành', 7, 'HARDSHIP_AREA'],
  ['NAM_NHAT', 'Sinh viên năm thứ nhất', 10, 'Hồ sơ trúng tuyển', false, 'Quy chế ký túc xá của Nhà trường', 8, 'NONE'],
  ['HOC_TAP_XUAT_SAC', 'Học tập và rèn luyện xuất sắc', 5, 'Bảng điểm và điểm rèn luyện học kỳ gần nhất', false, 'Quy chế ký túc xá của Nhà trường', 9, 'NONE'],
];
PRIORITIES.forEach(([code, name, weight, evidence, mandatory, legal, order, rule]) =>
  add('priority_category', { code, name, weight, active: true, description: null, evidence_required: evidence, evidence_mandatory: mandatory, legal_basis: legal, sort_order: order, auto_rule: rule, note: null }),
);
const PRIO_WEIGHT = Object.fromEntries(T.priority_category.map((p) => [p.id, p.weight]));

add('management_unit', {
  code: 'K1', name: 'Khu K1', short_name: 'K1', address: 'Số 175 Tây Sơn, Đống Đa, Hà Nội', hotline: '02435632201',
  email: 'ktx.k1@tlu.edu.vn', head_user_id: null, working_hours: '07:00–21:00 các ngày trong tuần', active: true, note: 'Một tòa nhà hỗn hợp nam và nữ theo từng tầng',
});
add('management_unit', {
  code: 'N23', name: 'Khu N2 + N3', short_name: 'N2-N3', address: 'Số 175 Tây Sơn, Đống Đa, Hà Nội', hotline: '02435632202',
  email: 'ktx.n23@tlu.edu.vn', head_user_id: null, working_hours: '07:00–21:00 các ngày trong tuần', active: true, note: 'N2 dành cho sinh viên nam, N3 dành cho sinh viên nữ',
});

// ---------------------------------------------------------------- tài khoản nội bộ
add('app_user', { id: 1 }, false); // admin được chèn riêng từ bản giữ lại để giữ liên kết Keycloak
IDS.app_user = 1;
const staff = { 1: [], 2: [] };
const tech = { 1: [], 2: [] };
function addUser(username, role, fullName, unit, extra = {}) {
  const row = add('app_user', {
    username, password_hash: null, role, full_name: fullName, phone: phone(), management_unit_id: unit,
    cross_unit_access: false, must_change_password: true, locked: false, failed_login_count: 0, locked_until: null,
    created_at: ts('2025-06-' + pad(between(5, 25))), security_version: 0, keycloak_user_id: null, ...extra,
  });
  return row;
}
const STAFF_DEF = [
  ['ql.k1.huong', 'Nguyễn Thị Thu Hương', 1, {}], ['ql.k1.tuan', 'Trần Anh Tuấn', 1, {}], ['ql.k1.lan', 'Phạm Thị Lan', 1, {}],
  ['ql.k1.hoa', 'Lê Thị Hòa', 1, { locked: true }],
  ['ql.n23.hung', 'Lê Văn Hùng', 2, {}], ['ql.n23.mai', 'Đỗ Thị Mai', 2, {}], ['ql.n23.long', 'Vũ Hoàng Long', 2, { cross_unit_access: true }],
];
STAFF_DEF.forEach(([u, n, unit, extra]) => staff[unit].push(addUser(u, 'STAFF', n, unit, extra).id));
const TECH_DEF = [
  ['kt.k1.dung', 'Hoàng Văn Dũng', 1, {}], ['kt.k1.thanh', 'Bùi Quang Thành', 1, {}], ['kt.k1.nam', 'Đặng Hữu Nam', 1, {}],
  ['kt.n23.khoa', 'Ngô Đăng Khoa', 2, {}], ['kt.n23.hai', 'Phan Văn Hải', 2, {}], ['kt.n23.tien', 'Trịnh Minh Tiến', 2, {}], ['kt.n23.binh', 'Dương Quốc Bình', 2, { locked: true }],
];
TECH_DEF.forEach(([u, n, unit, extra]) => tech[unit].push(addUser(u, 'TECHNICIAN', n, unit, extra).id));
const activeStaff = (unit) => staff[unit].filter((id) => !T.app_user.find((u) => u.id === id).locked);
const activeTech = (unit) => tech[unit].filter((id) => !T.app_user.find((u) => u.id === id).locked);
T.management_unit[0].head_user_id = staff[1][0];
T.management_unit[1].head_user_id = staff[2][0];
const ADMIN = 1;

// ---------------------------------------------------------------- sinh viên
const CAT_COUNTS = [
  ['active', 178], ['pending', 10], ['terminated', 6], ['exstudent', 25], ['appSubmitted', 14],
  ['appDraft', 4], ['appRejected', 4], ['appApproved', 6], ['appCancelled', 3], ['plain', 10],
];
const cats = shuffle(CAT_COUNTS.flatMap(([c, n]) => Array(n).fill(c)));
const COHORT_BY_CAT = {
  exstudent: [['K64', 50], ['K65', 40], ['K66', 10]],
  active: [['K64', 5], ['K65', 25], ['K66', 30], ['K67', 30], ['K68', 10]],
  default: [['K66', 20], ['K67', 30], ['K68', 50]],
};
const codeCounter = {};
const students = [];
cats.forEach((cat, index) => {
  const gender = chance(0.58) ? 'M' : 'F';
  const cohort = wpick(COHORT_BY_CAT[cat] || COHORT_BY_CAT.default);
  const cohortRow = T.student_cohort.find((c) => c.name === cohort);
  const school = wpick([[1, 88], [2, 4], [3, 4], [4, 4]]);
  let faculty;
  if (school === 1) faculty = wpick(FACULTY_WEIGHT);
  else faculty = { 2: 9, 3: 10, 4: 11 }[school];
  const yy = String(cohortRow.enrollment_year).slice(2);
  codeCounter[yy] = (codeCounter[yy] || 0) + between(1, 9);
  const prefix = school === 1 ? '51' : '52';
  const code = `${yy}${prefix}06${pad(codeCounter[yy], 4)}`;
  const hometown = chance(0.17)
    ? pick(HARD_HOMETOWN_IDS)
    : PROV_ID[pick(HOMETOWN_WEIGHT_PROVINCE)];
  const dobYear = cohortRow.enrollment_year - 18 - (chance(0.2) ? 1 : 0);
  const name = personName(gender);
  const user = addUser(code, 'STUDENT', name, null, {
    created_at: ts(`${cohortRow.enrollment_year}-08-${pad(between(1, 25))}`),
  });
  const relative = gender === 'M' ? pick(['Nguyễn Văn', 'Trần Quang', 'Phạm Đình', 'Lê Hữu']) : pick(['Nguyễn Thị', 'Trần Thị', 'Phạm Thị', 'Lê Thị']);
  const fatherName = `${name.split(' ')[0]} ${pick(MID_M)} ${pick(FIRST_M)}`;
  const motherName = `${name.split(' ')[0]} Thị ${pick(FIRST_F)}`;
  void relative;
  const emergency = chance(0.55) ? `${fatherName} (bố) - ${phone()}` : `${motherName} (mẹ) - ${phone()}`;
  add('student_profile', {
    user_id: user.id, student_code: code, gender, date_of_birth: `${dobYear}-${pad(between(1, 12))}-${pad(between(1, 28))}`,
    emergency_contact: emergency, school_id: school, faculty_id: faculty, cohort_id: cohortRow.id, hometown_id: hometown,
  }, false);
  students.push({ id: user.id, code, gender, name, cat, cohort, school, hometown, index, userRow: user });
});
// tài khoản bị khóa / đăng nhập sai
shuffle(students.filter((s) => s.cat === 'plain' || s.cat === 'exstudent')).slice(0, 4).forEach((s) => {
  s.userRow.locked = true;
  s.userRow.failed_login_count = 5;
});
shuffle(students.filter((s) => s.cat === 'active')).slice(0, 5).forEach((s) => {
  s.userRow.failed_login_count = between(1, 3);
});
const byCat = (c) => students.filter((s) => s.cat === c);
const STU = new Map(students.map((s) => [s.id, s]));

// ---------------------------------------------------------------- tòa nhà, phòng, giường
const BUILDINGS = [
  { id: 1, unit: 1, code: 'K1', name: 'Tòa K1', deposit: 1000000, floors: 5, ground: true, policy: 'MIXED', year: 2016, area: 5200, mgr: staff[1][0], hotline: '02435632211', note: 'Tầng 1–3 dành cho nam, tầng 4–5 dành cho nữ' },
  { id: 2, unit: 2, code: 'N2', name: 'Tòa N2', deposit: 2000000, floors: 4, ground: false, policy: 'M', year: 2012, area: 4100, mgr: staff[2][0], hotline: '02435632212', note: 'Tòa dành cho sinh viên nam' },
  { id: 3, unit: 2, code: 'N3', name: 'Tòa N3', deposit: 2000000, floors: 4, ground: false, policy: 'F', year: 2014, area: 4100, mgr: staff[2][1], hotline: '02435632213', note: 'Tòa dành cho sinh viên nữ' },
];
BUILDINGS.forEach((b) =>
  add('building', {
    id: b.id, management_unit_id: b.unit, code: b.code, name: b.name, deposit_amount: b.deposit, floor_count: b.floors,
    has_ground_floor: b.ground, gender_policy: b.policy, address: 'Số 175 Tây Sơn, Đống Đa, Hà Nội', commissioned_year: b.year,
    floor_area_m2: b.area, manager_user_id: b.mgr, hotline: b.hotline, open_time: '05:00', close_time: '23:30', note: b.note,
  }),
);
const floorGender = (b, floor) => (b.policy === 'MIXED' ? (floor <= 3 ? 'M' : 'F') : b.policy);
BUILDINGS.forEach((b) => {
  if (b.ground) {
    add('building_floor', { building_id: b.id, floor: 0, name: 'Tầng trệt', active: true, gender: null, shared_bathroom: false, common_room: true, note: 'Sảnh, phòng sinh hoạt chung và nhà để xe' });
  }
  for (let f = 1; f <= b.floors; f++) {
    add('building_floor', {
      building_id: b.id, floor: f, name: `Tầng ${f}`, active: true, gender: floorGender(b, f),
      shared_bathroom: b.code !== 'K1', common_room: f === 1, note: null,
    });
  }
});

const RTYPES = [
  ['LP-08', 'Phòng 8 người', 8, 1200000, 600000, 32.0, false, false, 'BUNK', false, 'Phòng tập thể 8 giường tầng, khu vệ sinh chung theo tầng', ['BUNK', 'WARDROBE', 'DESK', 'FAN', 'WIFI']],
  ['LP-06', 'Phòng 6 người', 6, 1500000, 750000, 28.0, false, false, 'BUNK', false, 'Phòng 6 giường tầng, có bình nóng lạnh', ['BUNK', 'WARDROBE', 'DESK', 'FAN', 'HEATER', 'WIFI']],
  ['LP-04', 'Phòng 4 người', 4, 1800000, 900000, 24.0, false, false, 'BUNK', false, 'Phòng 4 giường tầng, có điều hòa', ['BUNK', 'WARDROBE', 'DESK', 'AC', 'HEATER', 'WIFI']],
  ['LP-04K', 'Phòng 4 người khép kín', 4, 2400000, 1200000, 28.0, true, true, 'BUNK', false, 'Phòng 4 giường tầng, vệ sinh và ban công riêng', ['BUNK', 'WARDROBE', 'DESK', 'AC', 'HEATER', 'BALCONY', 'WIFI']],
  ['LP-02', 'Phòng 2 người dịch vụ', 2, 3600000, 1800000, 24.0, true, true, 'SINGLE', true, 'Phòng 2 giường đơn, bếp riêng, vệ sinh và ban công riêng', ['WARDROBE', 'DESK', 'AC', 'HEATER', 'BALCONY', 'KITCHEN', 'WIFI']],
];
RTYPES.forEach(([code, name, beds, pm, ps, area, bath, balcony, style, kitchen, desc, amens]) => {
  const row = add('room_type', { code, name, bed_count: beds, price_main: pm, price_summer: ps, area_m2: area, active: true, private_bathroom: bath, has_balcony: balcony, bed_style: style, has_kitchen: kitchen, description: desc });
  amens.forEach((a) => add('room_type_amenity', { room_type_id: row.id, amenity_id: AM[a] }, false));
});
const RT = Object.fromEntries(T.room_type.map((r) => [r.code, r]));

const LAYOUT = {
  K1: ['LP-04K', 'LP-04K', 'LP-06', 'LP-06', 'LP-02'],
  N2: ['LP-08', 'LP-08', 'LP-06', 'LP-04', 'LP-04'],
  N3: ['LP-08', 'LP-08', 'LP-06', 'LP-04', 'LP-04'],
};
const SPECIAL_ROOMS = {
  'K1-505': ['MAINTENANCE', 'Chống thấm trần và thay hệ thống điện, dự kiến hoàn thành trước 30/12/2026'],
  'N2-405': ['CLOSED', 'Ngừng sử dụng chờ cải tạo hệ thống điện toàn tầng'],
  'N3-404': ['MAINTENANCE', 'Thay mới bình nóng lạnh và đường ống nước'],
};
const ORIENT = ['N', 'S', 'E', 'W', 'NE', 'NW', 'SE', 'SW'];
const rooms = [];
BUILDINGS.forEach((b) => {
  for (let f = 1; f <= b.floors; f++) {
    for (let i = 0; i < 5; i++) {
      const type = RT[LAYOUT[b.code][i]];
      const code = `${b.code}-${f}${pad(i + 1)}`;
      const special = SPECIAL_ROOMS[code];
      const room = add('room', {
        building_id: b.id, room_type_id: type.id, floor: f, code, gender: floorGender(b, f), status: special ? special[0] : 'ACTIVE',
        leader_student_id: null, display_name: null, area_m2: +(type.area_m2 + (between(-10, 10) / 10)).toFixed(2),
        orientation: pick(ORIENT), electric_meter_no: `DH-${code}`, water_meter_no: `NH-${code}`,
        commissioned_on: `${b.year}-08-${pad(between(10, 28))}`, status_reason: special ? special[1] : null, note: null,
      });
      room._beds = [];
      room._type = type;
      room._b = b;
      for (let n = 1; n <= type.bed_count; n++) {
        const bed = add('bed', {
          room_id: room.id, code: `G${n}`, status: 'ACTIVE',
          position: type.bed_style === 'SINGLE' ? 'SINGLE' : (n % 2 === 1 ? 'LOWER' : 'UPPER'),
          asset_code: `GT-${code}-${n}`, note: null,
        });
        bed._room = room;
        room._beds.push(bed);
      }
      rooms.push(room);
    }
  }
});
const ROOM_BY_ID = new Map(rooms.map((r) => [r.id, r]));
const BED_BY_ID = new Map(rooms.flatMap((r) => r._beds).map((b) => [b.id, b]));

// chỗ ở được sắp trước: mỗi nhóm tòa-giới tính dựng hàng đợi sinh viên rồi điền phòng lần lượt
const occupant = new Map(); // bed.id -> student
function placeGroup(groupRooms, queue) {
  const fillable = groupRooms.filter((r) => r.status === 'ACTIVE');
  for (const room of fillable) {
    if (!queue.length) break;
    const frac = wpick([[1, 55], [0.75, 20], [0.5, 15], [0.25, 10]]);
    const target = Math.max(1, Math.ceil(room._beds.length * frac));
    for (let i = 0; i < target && queue.length; i++) {
      const bed = room._beds[i];
      occupant.set(bed.id, queue.shift());
    }
  }
  for (const room of fillable) {
    for (const bed of room._beds) {
      if (!queue.length) return;
      if (!occupant.has(bed.id)) occupant.set(bed.id, queue.shift());
    }
  }
}
const roomedStudents = shuffle(students.filter((s) => s.cat === 'active' || s.cat === 'pending'));
const GROUPS = { 'K1-M': [], 'K1-F': [], 'N2-M': [], 'N3-F': [] };
roomedStudents.forEach((s) => {
  const toK1 = chance(0.4);
  const key = s.gender === 'M' ? (toK1 ? 'K1-M' : 'N2-M') : (toK1 ? 'K1-F' : 'N3-F');
  GROUPS[key].push(s);
});
Object.entries(GROUPS).forEach(([key, queue]) => {
  const [bcode, g] = key.split('-');
  placeGroup(rooms.filter((r) => r._b.code === bcode && r.gender === g), queue.slice());
  void queue;
});
const roomOf = new Map(); // student.id -> bed
occupant.forEach((student, bedId) => roomOf.set(student.id, BED_BY_ID.get(bedId)));
if (roomOf.size !== roomedStudents.length) throw new Error(`Thiếu chỗ ở: xếp ${roomOf.size}/${roomedStudents.length}`);
// giường hỏng / thanh lý trong các phòng còn trống chỗ
const freeBedsForDamage = shuffle(rooms.filter((r) => r.status === 'ACTIVE').flatMap((r) => r._beds).filter((b) => !occupant.has(b.id) && b.code !== 'G1'));
freeBedsForDamage.slice(0, 6).forEach((b, i) => {
  b.status = 'BROKEN';
  b.note = ['Gãy khung giường, chờ hàn lại', 'Gãy thanh ngang tầng trên', 'Hỏng chân giường, chờ thay', 'Võng đệm, cần thay tấm lót', 'Mối hàn bị nứt', 'Cháy xém do chập điện'][i];
});
freeBedsForDamage.slice(6, 9).forEach((b) => {
  b.status = 'RETIRED';
  b.note = 'Đã thanh lý theo biên bản kiểm kê tài sản';
});

// ---------------------------------------------------------------- kho gửi đồ
const STORAGE = [
  [1, 'KHO-K1', 'Kho đồ khu K1', 1, 'Tầng trệt tòa K1', 120, staff[1][1]],
  [2, 'KHO-N2', 'Kho đồ tòa N2', 2, 'Tầng 1 tòa N2', 90, staff[2][1]],
  [2, 'KHO-N3', 'Kho đồ tòa N3', 3, 'Tầng 1 tòa N3', 90, staff[2][2]],
];
STORAGE.forEach(([unit, code, name, building, place, capacity, keeper]) =>
  add('storage_location', { management_unit_id: unit, code, name, active: true, building_id: building, place, capacity, keeper_user_id: keeper, note: null }),
);

// ---------------------------------------------------------------- học kỳ, đợt đăng ký
const SEM = [
  ['2025-2026', 'HK1', 'Học kỳ 1 năm học 2025-2026', '2025-08-25', '2026-01-18', '2025-09-30'],
  ['2025-2026', 'HK2', 'Học kỳ 2 năm học 2025-2026', '2026-01-26', '2026-06-14', '2026-02-28'],
  ['2025-2026', 'SUMMER', 'Học kỳ hè năm học 2025-2026', '2026-06-22', '2026-08-16', '2026-07-15'],
  ['2026-2027', 'HK1', 'Học kỳ 1 năm học 2026-2027', '2026-08-24', '2027-01-17', '2026-09-30'],
  ['2026-2027', 'HK2', 'Học kỳ 2 năm học 2026-2027', '2027-01-25', '2027-06-13', '2027-02-28'],
  ['2026-2027', 'SUMMER', 'Học kỳ hè năm học 2026-2027', '2027-06-21', '2027-08-15', '2027-07-15'],
];
SEM.forEach(([year, type, name, start, end, due]) =>
  add('semester', { academic_year: year, type, name, start_date: start, end_date: end, room_fee_due_date: due, note: null }),
);
const [S1, S2, S3, S4, S5, S6] = T.semester;
const PERIODS = [
  [S1, 'REGULAR', 'Đăng ký ở học kỳ 1 năm học 2025-2026', '2025-07-01 08:00:00+07', '2025-07-31 23:59:00+07', [1, 2]],
  [S2, 'REGULAR', 'Đăng ký ở học kỳ 2 năm học 2025-2026', '2025-12-10 08:00:00+07', '2026-01-05 23:59:00+07', [1, 2]],
  [S3, 'SUMMER', 'Đăng ký ở và gửi đồ kỳ hè 2026', '2026-05-04 08:00:00+07', '2026-05-24 23:59:00+07', [1, 2]],
  [S4, 'REGULAR', 'Đăng ký ở học kỳ 1 năm học 2026-2027', '2026-07-01 08:00:00+07', '2026-08-10 23:59:00+07', [1, 2]],
  [S5, 'REGULAR', 'Đăng ký ở học kỳ 2 năm học 2026-2027', '2026-11-16 08:00:00+07', '2026-12-08 23:59:00+07', [1, 2]],
  [S6, 'SUMMER', 'Đăng ký ở và gửi đồ kỳ hè 2027', '2026-11-09 08:00:00+07', '2026-12-20 23:59:00+07', [1, 2]],
];
PERIODS.forEach(([sem, kind, name, open, close, units], i) => {
  const p = add('registration_period', { semester_id: sem.id, kind, name, open_at: open, close_at: close, created_by: i % 2 ? staff[2][0] : staff[1][0] });
  units.forEach((u) => add('registration_period_unit', { period_id: p.id, management_unit_id: u }, false));
  const quotas = i === 4 ? [30, 35, 35] : i >= 3 ? [90, 80, 80] : [80, 70, 70];
  if (i !== 5) BUILDINGS.forEach((b, bi) => add('registration_quota', { period_id: p.id, building_id: b.id, quota: quotas[bi] }, false));
  else BUILDINGS.forEach((b, bi) => add('registration_quota', { period_id: p.id, building_id: b.id, quota: [40, 45, 45][bi] }, false));
});
const [P1, P2, P3, P4, P5, P6] = T.registration_period;

// ---------------------------------------------------------------- nhật ký tiện ích
const AUDIT = [];
const HIST = [];
function audit(actor, action, type, id, oldv, newv, reason, when) {
  AUDIT.push({ actor_id: actor, action, entity_type: type, entity_id: id, old_value: oldv ? new J(oldv) : null, new_value: newv ? new J(newv) : null, reason: reason || null, request_id: `req-${Math.floor(R() * 0xffffffffff).toString(16).padStart(10, '0')}`, created_at: when });
}
function hist(type, id, from, to, actor, reason, when) {
  HIST.push({ entity_type: type, entity_id: id, from_status: from, to_status: to, actor_id: actor, reason: reason || null, created_at: when });
}
const ROOM_UNIT = { 1: 1, 2: 2, 3: 2 };
const pickStaff = (unit) => pick(activeStaff(unit));
const pickTech = (unit) => pick(activeTech(unit));

// ---------------------------------------------------------------- đơn, hợp đồng, phân giường (kỳ HK1 2026-2027)
const apps = [];
const contracts = [];
const assignments = [];
function priorityRows(app, student, verifiedMode) {
  const picks = [];
  if (HARD_HOMETOWN_IDS.includes(student.hometown) && chance(0.8)) picks.push(T.priority_category.find((p) => p.code === 'VUNG_KHO_KHAN').id);
  if (student.cohort === 'K68' || student.cohort === 'K67' && chance(0.1)) picks.push(T.priority_category.find((p) => p.code === 'NAM_NHAT').id);
  if (chance(0.10)) {
    const wanted = pick(['HO_NGHEO', 'MO_COI', 'NGUOI_CO_CONG', 'KHUYET_TAT', 'LIET_SI_TB']);
    picks.push(T.priority_category.find((p) => p.code === wanted).id);
  }
  if (chance(0.08)) picks.push(T.priority_category.find((p) => p.code === 'HOC_TAP_XUAT_SAC').id);
  const unique = [...new Set(picks)];
  let score = 0;
  unique.forEach((cid) => {
    let verified = null;
    if (verifiedMode === 'all') verified = chance(0.9);
    else if (verifiedMode === 'some') verified = chance(0.5) ? chance(0.85) : null;
    if (verified !== false) score += PRIO_WEIGHT[cid];
    const verifiedBy = verified === null ? null : pickStaff(app.preferred_unit_id);
    add('application_priority', {
      application_id: app.id, category_id: cid, verified, verified_by: verifiedBy,
      verified_at: verified === null ? null : ts(addDays(tsDate(app.submitted_at), between(1, 4))),
      note: verified === true ? 'Đã đối chiếu hồ sơ bản gốc' : verified === false ? 'Hồ sơ minh chứng không hợp lệ, không tính điểm ưu tiên' : null,
    }, false);
  });
  app.priority_score = score;
}
function makeApp(student, period, status, opts = {}) {
  const sem = T.semester.find((s) => s.id === period.semester_id);
  const unit = opts.unit || ROOM_UNIT[(opts.bed ? opts.bed._room.building_id : 1)];
  const submittedDate = opts.submitted || addDays(period.open_at.slice(0, 10), between(1, Math.max(1, Math.min(20, diffDays(period.close_at.slice(0, 10), period.open_at.slice(0, 10)) - 1))));
  const app = add('room_application', {
    student_id: student.id, period_id: period.id, preferred_unit_id: unit, preferred_room_type_id: opts.typeId === undefined ? (chance(0.6) ? (opts.bed ? opts.bed._room.room_type_id : pick(T.room_type).id) : null) : opts.typeId,
    note: opts.note === undefined ? (chance(0.25) ? pick(['Em xin được ở cùng phòng với bạn cùng lớp.', 'Gia đình ở xa nên em mong được xét ưu tiên.', 'Em muốn ở tầng thấp vì sức khỏe.', 'Em đi học buổi tối nên xin phòng yên tĩnh.']) : null) : opts.note,
    priority_score: 0, status, status_reason: opts.reason || null, submitted_at: status === 'DRAFT' ? null : ts(submittedDate),
    created_at: status === 'DRAFT' ? ts(opts.created || submittedDate) : ts(addDays(submittedDate, -between(0, 1))),
  });
  app._student = student;
  app._period = period;
  app._sem = sem;
  const vm = status === 'DRAFT' ? 'none' : (status === 'SUBMITTED' ? 'some' : 'all');
  if (status !== 'DRAFT' || chance(0.4)) priorityRows(app, student, vm);
  apps.push(app);
  // lịch sử trạng thái
  if (status !== 'DRAFT') {
    hist('APPLICATION', app.id, null, 'DRAFT', student.id, null, app.created_at);
    hist('APPLICATION', app.id, 'DRAFT', 'SUBMITTED', student.id, null, app.submitted_at);
  } else hist('APPLICATION', app.id, null, 'DRAFT', student.id, null, app.created_at);
  return app;
}
function advanceApp(app, steps) {
  // steps: [[to, actor, reason, ts]]
  let prev = 'SUBMITTED';
  steps.forEach(([to, actor, reason, when]) => {
    hist('APPLICATION', app.id, prev, to, actor, reason, when);
    prev = to;
  });
}
function makeContract(app, student, start, end, status, createdTs, opts = {}) {
  const bed = opts.bed;
  const building = BUILDINGS.find((b) => b.id === bed._room.building_id);
  const deadline = addDays(tsDate(createdTs), 7);
  const c = add('dormitory_contract', {
    code: null, student_id: student.id, application_id: app.id, start_date: start, end_date: end, status,
    activation_deadline: ts(deadline, 23, 59), deposit_required: null, activated_at: null, activated_by: null, closed_at: null, status_reason: null, created_at: createdTs,
  });
  c._app = app;
  c._student = student;
  c._building = building;
  c._bed = bed;
  contracts.push(c);
  hist('CONTRACT', c.id, null, 'PENDING', opts.staff, null, createdTs);
  return c;
}
function activate(c, activatedTs, staffId) {
  c.status = 'ACTIVE';
  c.activated_at = activatedTs;
  c.activated_by = staffId;
  c.deposit_required = c._building.deposit;
  hist('CONTRACT', c.id, 'PENDING', 'ACTIVE', staffId, null, activatedTs);
  add('deposit_transaction', { contract_id: c.id, type: 'DEPOSIT', amount: c._building.deposit, maintenance_request_id: null, due_date: null, confirmed_by: staffId, created_at: activatedTs, note: 'Thu tiền đặt cọc khi kích hoạt hợp đồng' });
  audit(staffId, 'CONTRACT_ACTIVATE', 'CONTRACT', c.id, { status: 'PENDING' }, { status: 'ACTIVE', depositRequired: c._building.deposit }, null, activatedTs);
  audit(staffId, 'DEPOSIT_COLLECT', 'DEPOSIT_TRANSACTION', c.id, null, { contractId: c.id, amount: c._building.deposit }, null, activatedTs);
}
function makeAssignment(c, bed, startedTs, stayTs, staffId, extra = {}) {
  const type = bed._room._type;
  const a = add('room_assignment_history', {
    student_id: c.student_id, contract_id: c.id, bed_id: bed.id, kind: stayTs ? 'STAY' : 'HOLD', started_at: startedTs, stay_started_at: stayTs || null,
    ended_at: null, end_reason: null, price_main: type.price_main, price_summer: type.price_summer, note: null, created_by: staffId, ...extra,
  });
  a._bed = bed;
  a._contract = c;
  assignments.push(a);
  return a;
}
function closeAssignment(a, endTs, reason) {
  a.ended_at = endTs;
  a.end_reason = reason;
}

// ----- nhóm đang ở HK1 2026-2027
const activeStudents = shuffle(byCat('active'));
const flagged = new Map(); // student.id -> mảng cờ
const flag = (s, f) => {
  if (!flagged.has(s.id)) flagged.set(s.id, new Set());
  flagged.get(s.id).add(f);
};
const has = (s, f) => flagged.has(s.id) && flagged.get(s.id).has(f);
activeStudents.slice(0, 4).forEach((s) => flag(s, 'overdue'));
activeStudents.slice(4, 10).forEach((s) => flag(s, 'expiring'));
const normalActive = activeStudents.slice(10);
normalActive.forEach((s) => flag(s, chance(0.42) ? 'two' : 'one'));
const twoSem = normalActive.filter((s) => has(s, 'two'));
const oneSem = normalActive.filter((s) => has(s, 'one'));
shuffle(normalActive).slice(0, 4).forEach((s) => flag(s, 'transfer'));
shuffle(activeStudents).slice(0, 35).forEach((s) => flag(s, 'history'));
const OVERDUE_END = ['2026-11-14', '2026-11-16', '2026-11-18', '2026-11-20'];
const EXPIRING_END = ['2026-11-26', '2026-11-28', '2026-11-30', '2026-12-02', '2026-12-04', '2026-12-06'];
let overdueIdx = 0;
let expiringIdx = 0;
const activeContracts = [];
activeStudents.forEach((s) => {
  const bed = roomOf.get(s.id);
  const unit = ROOM_UNIT[bed._room.building_id];
  const staffId = pickStaff(unit);
  const submitted = `2026-07-${pad(between(2, 28))}`;
  const app = makeApp(s, P4, 'ASSIGNED', { bed, unit, submitted });
  const approvedTs = ts(addDays(submitted, between(2, 8)));
  const createdDate = addDays(tsDate(approvedTs), between(1, 6));
  const createdTs = ts(createdDate);
  advanceApp(app, [['APPROVED', staffId, null, approvedTs], ['ASSIGNED', staffId, null, createdTs]]);
  let end = '2027-01-17';
  if (has(s, 'two')) end = '2027-06-13';
  if (has(s, 'overdue')) end = OVERDUE_END[overdueIdx++];
  if (has(s, 'expiring')) end = EXPIRING_END[expiringIdx++];
  const c = makeContract(app, s, '2026-08-24', end, 'PENDING', createdTs, { bed, staff: staffId });
  const activatedDate = addDays(createdDate, between(1, 6));
  const activatedTs = ts(activatedDate);
  const a = makeAssignment(c, bed, createdTs, activatedTs, staffId);
  activate(c, activatedTs, staffId);
  audit(staffId, 'APPLICATION_ASSIGN', 'APPLICATION', app.id, { status: 'APPROVED' }, { status: 'ASSIGNED', bedId: bed.id }, null, createdTs);
  c._assign = a;
  c._unit = unit;
  c._staff = staffId;
  activeContracts.push(c);
});
const contractOf = new Map(activeContracts.map((c) => [c._student.id, c]));

// ----- chuyển phòng đã duyệt (4)
const transferStudents = activeStudents.filter((s) => has(s, 'transfer'));
function freeBedFor(student, building) {
  const candidates = rooms.filter((r) => r.status === 'ACTIVE' && r.gender === student.gender && (!building || r.building_id === building) && ROOM_UNIT[r.building_id] === ROOM_UNIT[roomOf.get(student.id)._room.building_id] && r.id !== roomOf.get(student.id)._room.id)
    .flatMap((r) => r._beds)
    .filter((b) => b.status === 'ACTIVE' && !occupant.has(b.id) && !reservedBeds.has(b.id));
  return candidates.length ? pick(candidates) : null;
}
const reservedBeds = new Set();
const transferRequests = [];
transferStudents.forEach((s) => {
  const c = contractOf.get(s.id);
  const target = freeBedFor(s);
  if (!target) return;
  reservedBeds.add(target.id);
  const createdDate = `2026-10-${pad(between(1, 12))}`;
  const decidedTs = ts(addDays(createdDate, between(1, 3)));
  const req = add('room_transfer_request', {
    contract_id: c.id, reason: pick(['Phòng hiện tại ồn do gần đường lớn, em xin chuyển sang phòng yên tĩnh hơn.', 'Em xin chuyển để ở cùng nhóm bạn cùng lớp thuận tiện học nhóm.', 'Giường hiện tại bị dột khi mưa, em xin chuyển phòng.', 'Em xin chuyển xuống tầng thấp vì chấn thương chân.']),
    status: 'APPROVED', target_bed_id: target.id, decided_by: c._staff, decided_at: decidedTs, decision_reason: null, created_at: ts(createdDate),
  });
  hist('TRANSFER', req.id, null, 'PENDING', s.id, null, req.created_at);
  hist('TRANSFER', req.id, 'PENDING', 'APPROVED', c._staff, null, decidedTs);
  audit(c._staff, 'ROOM_TRANSFER', 'TRANSFER', req.id, { bedId: c._bed.id }, { bedId: target.id }, null, decidedTs);
  const moveTs = ts(addDays(tsDate(decidedTs), 1));
  closeAssignment(c._assign, moveTs, 'TRANSFER');
  const next = makeAssignment(c, target, moveTs, moveTs, c._staff, { note: 'Chuyển phòng theo yêu cầu đã duyệt' });
  c._bed = target;
  c._assign2 = next;
  occupant.delete(roomOf.get(s.id).id);
  occupant.set(target.id, s);
  roomOf.set(s.id, target);
  transferRequests.push(req);
});
// ----- các yêu cầu chuyển phòng khác
const otherActive = normalActive.filter((s) => !has(s, 'transfer'));
function makeTransfer(student, status, extra = {}) {
  const c = contractOf.get(student.id);
  const createdDate = `2026-11-${pad(between(1, 20))}`;
  const row = add('room_transfer_request', {
    contract_id: c.id, reason: pick(['Em xin chuyển phòng vì xung đột sinh hoạt với bạn cùng phòng.', 'Phòng hiện tại thiếu ánh sáng, ảnh hưởng đến việc học.', 'Em xin chuyển sang tòa gần giảng đường hơn.', 'Gia đình có việc, em xin chuyển về phòng ít người hơn.']),
    status, target_bed_id: null, decided_by: null, decided_at: null, decision_reason: null, created_at: ts(createdDate), ...extra,
  });
  hist('TRANSFER', row.id, null, 'PENDING', student.id, null, row.created_at);
  if (status === 'REJECTED') {
    row.decided_by = c._staff;
    row.decided_at = ts(addDays(createdDate, between(1, 2)));
    row.decision_reason = pick(['Hiện chưa có giường trống phù hợp trong khu.', 'Lý do chưa đủ căn cứ, đề nghị trao đổi với trưởng phòng.', 'Cần hoàn tất nghĩa vụ tài chính trước khi chuyển.']);
    hist('TRANSFER', row.id, 'PENDING', 'REJECTED', c._staff, row.decision_reason, row.decided_at);
  } else if (status === 'CANCELLED') {
    hist('TRANSFER', row.id, 'PENDING', 'CANCELLED', student.id, null, ts(addDays(createdDate, 1)));
  }
  return row;
}
shuffle(otherActive).slice(0, 8).forEach((s) => makeTransfer(s, 'PENDING'));
shuffle(otherActive).slice(8, 11).forEach((s) => makeTransfer(s, 'REJECTED'));
// chỉ một đơn PENDING mỗi hợp đồng nên các nhóm dưới lấy sinh viên khác nhau
const usedForTransfer = new Set(T.room_transfer_request.map((r) => r.contract_id));
shuffle(otherActive.filter((s) => !usedForTransfer.has(contractOf.get(s.id).id))).slice(0, 2).forEach((s) => makeTransfer(s, 'CANCELLED'));

// ----- gia hạn
function makeExtension(student, status, oldEnd, newEnd, source, extra = {}) {
  const c = contractOf.get(student.id);
  const createdDate = extra.createdDate || `2026-${pick(['10', '11'])}-${pad(between(1, 20))}`;
  const row = add('contract_extension_request', {
    contract_id: c.id, source, old_end_date: oldEnd, new_end_date: newEnd,
    reason: source === 'SUMMER_STAY' ? 'Gia hạn để ở lại trong kỳ hè theo lựa chọn đã được duyệt.' : pick(['Em còn lịch học đến hết học kỳ 2 nên xin gia hạn hợp đồng.', 'Em xin tiếp tục ở để thuận tiện đi thực tập tại Hà Nội.', 'Gia đình ở xa, em xin gia hạn đến hết năm học.']),
    status, decided_by: null, decided_at: null, decision_reason: null, created_at: ts(createdDate),
  });
  hist('EXTENSION', row.id, null, 'PENDING', student.id, null, row.created_at);
  const decidedTs = ts(addDays(createdDate, between(1, 4)));
  if (status === 'APPROVED') {
    row.decided_by = c._staff;
    row.decided_at = decidedTs;
    hist('EXTENSION', row.id, 'PENDING', 'APPROVED', c._staff, null, decidedTs);
    audit(c._staff, 'CONTRACT_EXTENSION_APPROVE', 'EXTENSION', row.id, { endDate: oldEnd }, { endDate: newEnd }, null, decidedTs);
  } else if (status === 'REJECTED') {
    row.decided_by = c._staff;
    row.decided_at = decidedTs;
    row.decision_reason = pick(['Chưa đến thời hạn xét gia hạn theo kế hoạch của khu.', 'Sinh viên còn khoản phí chưa thanh toán.', 'Khu hết chỗ cho học kỳ tiếp theo.']);
    hist('EXTENSION', row.id, 'PENDING', 'REJECTED', c._staff, row.decision_reason, decidedTs);
    audit(c._staff, 'CONTRACT_EXTENSION_REJECT', 'EXTENSION', row.id, { status: 'PENDING' }, { status: 'REJECTED' }, row.decision_reason, decidedTs);
  } else if (status === 'CANCELLED') {
    hist('EXTENSION', row.id, 'PENDING', 'CANCELLED', student.id, null, ts(addDays(createdDate, 1)));
  }
  return row;
}
const oneSemPool = shuffle(oneSem.filter((s) => !usedForTransfer.has(contractOf.get(s.id).id)));
oneSemPool.slice(0, 6).forEach((s) => makeExtension(s, 'PENDING', '2027-01-17', '2027-06-13', 'STUDENT'));
oneSemPool.slice(6, 9).forEach((s) => makeExtension(s, 'REJECTED', '2027-01-17', '2027-06-13', 'STUDENT'));
oneSemPool.slice(9, 11).forEach((s) => makeExtension(s, 'CANCELLED', '2027-01-17', '2027-06-13', 'STUDENT'));
const twoPool = shuffle(twoSem.filter((s) => !usedForTransfer.has(contractOf.get(s.id).id)));
const extApproved = twoPool.slice(0, 5);
extApproved.forEach((s) => makeExtension(s, 'APPROVED', '2027-01-17', '2027-06-13', 'STUDENT', { createdDate: `2026-10-${pad(between(5, 20))}` }));

// ----- lựa chọn kỳ hè 2027 (đang mở đăng ký)
const summerPool = twoPool.slice(5);
function makeSummerChoice(student, choice, status, semester, createdDate, extra = {}) {
  const c = extra.contract || contractOf.get(student.id);
  const row = add('summer_choice', { contract_id: c.id, semester_id: semester.id, choice, status, decided_by: null, decided_at: null, created_at: ts(createdDate) });
  hist('SUMMER_CHOICE', row.id, null, 'PENDING', c._student.id, null, row.created_at);
  if (status !== 'PENDING') {
    const decidedTs = ts(addDays(createdDate, between(1, 5)));
    const decider = extra.decider || c._staff;
    if (status !== 'CANCELLED') {
      row.decided_by = decider;
      row.decided_at = decidedTs;
    }
    hist('SUMMER_CHOICE', row.id, 'PENDING', status, status === 'CANCELLED' ? c._student.id : decider, null, decidedTs);
    const action = status === 'APPROVED' ? 'SUMMER_CHOICE_APPROVE' : status === 'REJECTED' ? 'SUMMER_CHOICE_REJECT' : 'SUMMER_CHOICE_CANCEL';
    audit(status === 'CANCELLED' ? c._student.id : decider, action, 'SUMMER_CHOICE', row.id, { status: 'PENDING' }, { status, choice }, null, decidedTs);
  }
  row._contract = c;
  return row;
}
const summerChoices = [];
summerPool.slice(0, 12).forEach((s) => summerChoices.push(makeSummerChoice(s, pick(['STAY', 'TEMP_LEAVE', 'LEAVE']), 'PENDING', S6, `2026-11-${pad(between(10, 21))}`)));
summerPool.slice(12, 18).forEach((s) => {
  const row = makeSummerChoice(s, 'STAY', 'APPROVED', S6, `2026-11-${pad(between(10, 17))}`);
  summerChoices.push(row);
  const c = row._contract;
  makeExtension(s, 'APPROVED', '2027-06-13', '2027-08-15', 'SUMMER_STAY', { createdDate: tsDate(row.created_at) });
  c.end_date = '2027-08-15';
});
summerPool.slice(18, 21).forEach((s) => summerChoices.push(makeSummerChoice(s, 'TEMP_LEAVE', 'APPROVED', S6, `2026-11-${pad(between(10, 17))}`)));
summerPool.slice(21, 24).forEach((s) => summerChoices.push(makeSummerChoice(s, pick(['STAY', 'LEAVE']), 'REJECTED', S6, `2026-11-${pad(between(10, 17))}`)));
summerPool.slice(24, 26).forEach((s) => summerChoices.push(makeSummerChoice(s, 'TEMP_LEAVE', 'CANCELLED', S6, `2026-11-${pad(between(10, 17))}`)));

// ----- đơn học kỳ 2 (đang mở): gia hạn bằng đơn mới của sinh viên đang ở hợp đồng ngắn
const renewals = shuffle(oneSem).filter((s) => true);
renewals.slice(0, 18).forEach((s) => {
  const bed = roomOf.get(s.id);
  makeApp(s, P5, 'SUBMITTED', { bed, unit: ROOM_UNIT[bed._room.building_id], submitted: `2026-11-${pad(between(16, 21))}` });
});
renewals.slice(18, 26).forEach((s) => {
  const bed = roomOf.get(s.id);
  const staffId = contractOf.get(s.id)._staff;
  const sub = `2026-11-${pad(between(16, 19))}`;
  const app = makeApp(s, P5, 'APPROVED', { bed, unit: ROOM_UNIT[bed._room.building_id], submitted: sub });
  advanceApp(app, [['APPROVED', staffId, null, ts(addDays(sub, 2))]]);
});
renewals.slice(26, 28).forEach((s) => {
  const bed = roomOf.get(s.id);
  const staffId = contractOf.get(s.id)._staff;
  const sub = `2026-11-${pad(between(16, 18))}`;
  const app = makeApp(s, P5, 'REJECTED', { bed, unit: ROOM_UNIT[bed._room.building_id], submitted: sub, reason: 'Sinh viên chưa hoàn thành nghĩa vụ đóng phí phòng học kỳ hiện tại.' });
  advanceApp(app, [['REJECTED', staffId, app.status_reason, ts(addDays(sub, 2))]]);
});

// ----- chờ kích hoạt (giữ giường) của đợt HK2
const pendingContracts = [];
const DEADLINE_OFFSETS = [2, 2, 3, 3, 4, 4, 5, 5, 6, 7];
byCat('pending').forEach((s, i) => {
  const bed = roomOf.get(s.id);
  const unit = ROOM_UNIT[bed._room.building_id];
  const staffId = pickStaff(unit);
  const sub = '2026-11-16';
  const app = makeApp(s, P5, 'ASSIGNED', { bed, unit, submitted: sub });
  const approvedTs = ts('2026-11-16', 18, 30);
  const created = addDays(NOW_DATE, DEADLINE_OFFSETS[i] - 7);
  const createdTs = ts(created, 9, 0);
  advanceApp(app, [['APPROVED', staffId, null, approvedTs], ['ASSIGNED', staffId, null, createdTs]]);
  const c = makeContract(app, s, '2027-01-25', '2027-06-13', 'PENDING', createdTs, { bed, staff: staffId });
  c.activation_deadline = ts(addDays(created, 7), 23, 59);
  makeAssignment(c, bed, createdTs, null, staffId);
  audit(staffId, 'APPLICATION_ASSIGN', 'APPLICATION', app.id, { status: 'APPROVED' }, { status: 'ASSIGNED', bedId: bed.id }, null, createdTs);
  c._unit = unit;
  c._staff = staffId;
  pendingContracts.push(c);
});

// ----- sinh viên đăng ký mới đợt HK2 (không có chỗ)
function freeBedAnywhere(gender) {
  const candidates = rooms.filter((r) => r.status === 'ACTIVE' && r.gender === gender).flatMap((r) => r._beds).filter((b) => b.status === 'ACTIVE');
  return pick(candidates);
}
byCat('appSubmitted').forEach((s) => {
  const bed = freeBedAnywhere(s.gender);
  makeApp(s, P5, 'SUBMITTED', { bed, unit: ROOM_UNIT[bed._room.building_id], submitted: `2026-11-${pad(between(16, 21))}` });
});
byCat('appDraft').forEach((s) => {
  const bed = freeBedAnywhere(s.gender);
  makeApp(s, P5, 'DRAFT', { bed, unit: ROOM_UNIT[bed._room.building_id], submitted: '2026-11-20', created: '2026-11-20' });
});
byCat('appRejected').forEach((s) => {
  const bed = freeBedAnywhere(s.gender);
  const sub = `2026-11-${pad(between(16, 19))}`;
  const unit = ROOM_UNIT[bed._room.building_id];
  const reason = pick(['Hồ sơ minh chứng diện ưu tiên không hợp lệ và đã hết chỉ tiêu của khu.', 'Sinh viên không thuộc đối tượng được ở ký túc xá trong học kỳ này.', 'Khu đã hết chỉ tiêu cho đối tượng đăng ký.']);
  const app = makeApp(s, P5, 'REJECTED', { bed, unit, submitted: sub, reason });
  advanceApp(app, [['REJECTED', pickStaff(unit), reason, ts(addDays(sub, 2))]]);
});
byCat('appApproved').forEach((s) => {
  const bed = freeBedAnywhere(s.gender);
  const unit = ROOM_UNIT[bed._room.building_id];
  const sub = `2026-11-${pad(between(16, 19))}`;
  const app = makeApp(s, P5, 'APPROVED', { bed, unit, submitted: sub });
  advanceApp(app, [['APPROVED', pickStaff(unit), null, ts(addDays(sub, between(1, 2)))]]);
});
byCat('appCancelled').forEach((s) => {
  const bed = freeBedAnywhere(s.gender);
  const unit = ROOM_UNIT[bed._room.building_id];
  const sub = `2026-11-${pad(between(16, 19))}`;
  const app = makeApp(s, P5, 'CANCELLED', { bed, unit, submitted: sub, reason: 'Sinh viên tự hủy đơn vì đã thuê nhà ngoài.' });
  advanceApp(app, [['CANCELLED', s.id, app.status_reason, ts(addDays(sub, 3))]]);
});

// ----- sinh viên chưa ở: đơn kỳ HK1 bị từ chối / hủy / quá hạn kích hoạt
const plain = byCat('plain');
const autoCancelled = plain.slice(0, 3);
autoCancelled.forEach((s) => {
  const bed = freeBedAnywhere(s.gender);
  const unit = ROOM_UNIT[bed._room.building_id];
  const staffId = pickStaff(unit);
  const sub = `2026-07-${pad(between(10, 25))}`;
  const app = makeApp(s, P4, 'CANCELLED', { bed, unit, submitted: sub, reason: 'Hết hạn kích hoạt hợp đồng, hệ thống tự hủy đơn' });
  const approvedTs = ts(addDays(sub, 3));
  const createdTs = ts('2026-08-01');
  advanceApp(app, [['APPROVED', staffId, null, approvedTs], ['ASSIGNED', staffId, null, createdTs], ['CANCELLED', null, app.status_reason, ts('2026-08-09', 0, 5)]]);
  const c = makeContract(app, s, '2026-08-24', '2027-01-17', 'CANCELLED', createdTs, { bed, staff: staffId });
  c.closed_at = ts('2026-08-09', 0, 5);
  c.status_reason = 'Quá hạn kích hoạt hợp đồng';
  hist('CONTRACT', c.id, 'PENDING', 'CANCELLED', null, c.status_reason, c.closed_at);
  const a = makeAssignment(c, bed, createdTs, null, staffId);
  closeAssignment(a, c.closed_at, 'HOLD_EXPIRED');
  audit(null, 'CONTRACT_AUTO_CANCEL', 'CONTRACT', c.id, { status: 'PENDING' }, { status: 'CANCELLED' }, c.status_reason, c.closed_at);
  audit(null, 'APPLICATION_AUTO_CANCEL', 'APPLICATION', app.id, { status: 'ASSIGNED' }, { status: 'CANCELLED' }, app.status_reason, c.closed_at);
});
plain.slice(3, 5).forEach((s) => {
  const bed = freeBedAnywhere(s.gender);
  const unit = ROOM_UNIT[bed._room.building_id];
  const sub = `2026-07-${pad(between(10, 25))}`;
  const reason = 'Quá chỉ tiêu của khu, chưa đủ điểm ưu tiên';
  const app = makeApp(s, P4, 'REJECTED', { bed, unit, submitted: sub, reason });
  advanceApp(app, [['REJECTED', pickStaff(unit), reason, ts(addDays(sub, 4))]]);
});
plain.slice(5, 7).forEach((s) => {
  const bed = freeBedAnywhere(s.gender);
  const unit = ROOM_UNIT[bed._room.building_id];
  const sub = `2026-07-${pad(between(10, 25))}`;
  const app = makeApp(s, P4, 'CANCELLED', { bed, unit, submitted: sub, reason: 'Sinh viên rút đơn' });
  advanceApp(app, [['CANCELLED', s.id, app.status_reason, ts(addDays(sub, 2))]]);
});

// ----- sinh viên đã trả phòng trong học kỳ hiện tại
const terminatedList = byCat('terminated');
const moveOuts = [];
const tickets = [];
const MAINT_DESC = {
  ELECTRIC: ['Ổ cắm gần giường bị chập chờn, cắm sạc không vào điện.', 'Bóng đèn hành lang phòng nhấp nháy liên tục.', 'Quạt trần kêu to và quay chậm.', 'Cầu dao tổng của phòng hay bị nhảy khi dùng ấm điện.', 'Công tắc đèn khu vệ sinh bị hỏng, bấm không sáng.', 'Điều hòa không lạnh, chỉ có gió.'],
  WATER: ['Vòi nước ở bồn rửa mặt bị rò rỉ nhỏ giọt cả ngày.', 'Bồn cầu bị tắc nghẹt, nước không thoát.', 'Bình nóng lạnh không lên nhiệt.', 'Mất nước tại tầng từ chiều nay.', 'Ống thoát sàn nhà vệ sinh có mùi hôi, thoát chậm.', 'Vòi sen bị gãy tay cầm, nước phun lung tung.'],
  FURNITURE: ['Khung giường tầng trên bị lung lay khi nằm.', 'Cánh tủ cá nhân bị bung bản lề.', 'Mặt bàn học bị bong tróc và nứt.', 'Cửa sổ không đóng kín, gió lùa mạnh khi trời lạnh.', 'Rèm cửa bị đứt móc, không kéo được.'],
  DOOR_LOCK: ['Khóa cửa phòng bị kẹt, chìa không xoay được.', 'Bản lề cửa phòng lỏng, cửa bị xệ.', 'Chốt cửa ban công bị gãy.', 'Thẻ từ không mở được cửa tầng.'],
  OTHER: ['Có mối mọt ở chân tủ gỗ cuối phòng.', 'Trần nhà bị thấm nước sau cơn mưa, loang ố.', 'Tường cạnh giường bị nấm mốc lan rộng.', 'Muỗi nhiều do hệ thống lưới chống muỗi bị rách.'],
};
const COMMON_DESC = ['Đèn hành lang bị cháy, khu vực tối vào ban đêm.', 'Máy lọc nước ở sảnh không ra nước nóng.', 'Cửa thang bộ bị kẹt, khó mở.', 'Vòi nước khu phơi đồ bị rò rỉ.', 'Đèn cầu thang tầng bị hỏng.'];
function makeTicket(o) {
  const room = o.room || null;
  const status = o.status;
  const created = o.created;
  const t = add('maintenance_request', {
    code: null, requester_id: o.requester, room_id: room ? room.id : null, building_id: room ? null : o.building, floor: room ? null : o.floor,
    category: o.category, severity: o.severity, description: o.description, status, assigned_to: o.assigned || null,
    resolved_at: o.resolvedAt || null, closed_at: o.closedAt || null, cause: o.cause || null, repair_cost: o.cost || null, cost_note: o.costNote || null,
    cost_decided_by: o.costBy || null, cost_decided_at: o.costAt || null, created_at: created,
  });
  tickets.push(t);
  return t;
}
// các yêu cầu sửa chữa
const occupiedRooms = rooms.filter((r) => r._beds.some((b) => occupant.has(b.id)));
function randomOccupantOf(room) {
  const list = room._beds.filter((b) => occupant.has(b.id)).map((b) => occupant.get(b.id));
  return pick(list);
}
function severityFor(cat) {
  const base = MAINT_CATS.find((c) => c[0] === cat)[2];
  return chance(0.7) ? base : pick(['LOW', 'MEDIUM', 'HIGH']);
}
const TICKET_STATUS_PLAN = [
  ['OPEN', 12], ['ASSIGNED', 8], ['IN_PROGRESS', 8], ['RESOLVED', 6], ['CLOSED_WEAR', 22], ['CLOSED_MISUSE', 12], ['REJECTED', 5], ['CANCELLED', 5],
];
const misuseTickets = [];
TICKET_STATUS_PLAN.forEach(([plan, count]) => {
  for (let i = 0; i < count; i++) {
    const common = plan === 'CLOSED_MISUSE' || plan === 'CLOSED_WEAR' ? false : chance(0.15);
    const category = common ? pick(['ELECTRIC', 'WATER', 'OTHER', 'DOOR_LOCK']) : wpick([['ELECTRIC', 28], ['WATER', 28], ['FURNITURE', 20], ['DOOR_LOCK', 14], ['OTHER', 10]]);
    let room = null;
    let building = null;
    let floor = null;
    let requester;
    let unit;
    if (common) {
      const b = pick(BUILDINGS);
      building = b.id;
      floor = between(1, b.floors);
      unit = b.unit;
      requester = chance(0.5) ? pickStaff(unit) : pick(students.filter((s) => s.cat === 'active' && roomOf.has(s.id) && roomOf.get(s.id)._room.building_id === b.id) || [students[0]]).id;
    } else {
      room = pick(occupiedRooms);
      unit = ROOM_UNIT[room.building_id];
      requester = randomOccupantOf(room).id;
    }
    let status;
    let created;
    let resolvedAt = null;
    let closedAt = null;
    let cause = null;
    let cost = null;
    let costNote = null;
    let costBy = null;
    let costAt = null;
    let assigned = null;
    const desc = common ? pick(COMMON_DESC) : pick(MAINT_DESC[category]);
    if (plan === 'OPEN') {
      status = 'OPEN';
      created = ts(addDays(NOW_DATE, -between(0, 4)));
    } else if (plan === 'ASSIGNED') {
      status = 'ASSIGNED';
      created = ts(addDays(NOW_DATE, -between(1, 6)));
      assigned = pickTech(unit);
    } else if (plan === 'IN_PROGRESS') {
      status = 'IN_PROGRESS';
      created = ts(addDays(NOW_DATE, -between(2, 9)));
      assigned = pickTech(unit);
    } else if (plan === 'RESOLVED') {
      status = 'RESOLVED';
      const resolvedDate = addDays(NOW_DATE, -between(0, 2));
      created = ts(addDays(resolvedDate, -between(1, 4)));
      resolvedAt = ts(resolvedDate);
      assigned = pickTech(unit);
    } else if (plan === 'CLOSED_WEAR' || plan === 'CLOSED_MISUSE') {
      status = 'CLOSED';
      const closedDate = addDays(NOW_DATE, -(plan === 'CLOSED_MISUSE' ? between(1, 40) : between(3, 85)));
      const resolvedDate = addDays(closedDate, -between(1, 3));
      created = ts(addDays(resolvedDate, -between(1, 5)));
      resolvedAt = ts(resolvedDate);
      closedAt = ts(closedDate);
      assigned = pickTech(unit);
      if (!room) {
        // phiếu khu vực chung không được ghi nguyên nhân
      } else if (plan === 'CLOSED_MISUSE') {
        cause = 'MISUSE';
        cost = pick([120000, 180000, 250000, 300000, 450000, 600000, 800000]);
        costNote = 'Hư hỏng do sử dụng sai cách, khấu trừ tiền cọc theo quy định';
        costBy = pickStaff(unit);
        costAt = ts(closedDate);
      } else {
        cause = 'WEAR';
        costNote = 'Hao mòn tự nhiên trong quá trình sử dụng, không trừ cọc';
        costBy = pickStaff(unit);
        costAt = ts(closedDate);
      }
    } else if (plan === 'REJECTED') {
      status = 'REJECTED';
      created = ts(addDays(NOW_DATE, -between(3, 30)));
      closedAt = ts(addDays(tsDate(created), 1));
    } else {
      status = 'CANCELLED';
      created = ts(addDays(NOW_DATE, -between(3, 30)));
      closedAt = ts(addDays(tsDate(created), 1));
    }
    const t = makeTicket({ room, building, floor, requester, category, severity: severityFor(category), description: desc, status, created, assigned, resolvedAt, closedAt, cause, cost, costNote, costBy, costAt });
    t._unit = unit;
    // lịch sử trạng thái
    const staffId = pickStaff(unit);
    hist('MAINTENANCE', t.id, null, 'OPEN', requester, null, created);
    const day = (n) => ts(addDays(tsDate(created), n));
    if (['ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].includes(status)) hist('MAINTENANCE', t.id, 'OPEN', 'ASSIGNED', staffId, null, day(0));
    if (['IN_PROGRESS', 'RESOLVED', 'CLOSED'].includes(status)) hist('MAINTENANCE', t.id, 'ASSIGNED', 'IN_PROGRESS', assigned, null, day(1));
    if (['RESOLVED', 'CLOSED'].includes(status)) hist('MAINTENANCE', t.id, 'IN_PROGRESS', 'RESOLVED', assigned, null, resolvedAt);
    if (status === 'CLOSED') {
      hist('MAINTENANCE', t.id, 'RESOLVED', 'CLOSED', cause ? costBy : null, cause ? null : 'Tự động đóng sau thời hạn chờ phản hồi', closedAt);
      if (cause) audit(costBy, 'MAINTENANCE_CONCLUDE', 'MAINTENANCE', t.id, { status: 'RESOLVED' }, { status: 'CLOSED', cause, repairCost: cost }, null, closedAt);
      else audit(null, 'MAINTENANCE_AUTO_CLOSE', 'MAINTENANCE', t.id, { status: 'RESOLVED' }, { status: 'CLOSED' }, null, closedAt);
    }
    if (status === 'REJECTED') hist('MAINTENANCE', t.id, 'OPEN', 'REJECTED', staffId, pick(['Sự cố do sinh viên tự gây ra trong phạm vi tự bảo quản.', 'Yêu cầu trùng với phiếu đang xử lý.', 'Hạng mục không thuộc phạm vi bảo trì của ký túc xá.']), closedAt);
    if (status === 'CANCELLED') hist('MAINTENANCE', t.id, 'OPEN', 'CANCELLED', requester, 'Sinh viên đã tự khắc phục', closedAt);
    if (cause === 'MISUSE') misuseTickets.push(t);
  }
});

// ----- sổ đặt cọc: trừ cọc, bù cọc theo phiếu MISUSE
const NOW_TS = `${NOW_DATE} 21:00:00+07`;
const deductions = [];
let misuseCase = 0;
misuseTickets.forEach((t) => {
  const room = ROOM_BY_ID.get(t.room_id);
  const occupantsHere = room._beds.filter((b) => occupant.has(b.id)).map((b) => occupant.get(b.id)).filter((s) => contractOf.has(s.id));
  if (!occupantsHere.length) return;
  const c = contractOf.get(pick(occupantsHere).id);
  // chỉ một khoản trừ cho mỗi (hợp đồng, phiếu)
  const dueDate = addDays(tsDate(t.closed_at), 7);
  const row = add('deposit_transaction', {
    contract_id: c.id, type: 'DEDUCTION', amount: -t.repair_cost, maintenance_request_id: t.id, due_date: dueDate, confirmed_by: null,
    created_at: t.closed_at, note: `Khấu trừ chi phí sửa chữa phiếu ${'{code}'} do sử dụng sai cách`,
  });
  row._ticket = t;
  deductions.push(row);
  audit(t.cost_decided_by, 'DEPOSIT_DEDUCT', 'DEPOSIT_TRANSACTION', row.id, null, { contractId: c.id, amount: -t.repair_cost }, null, t.closed_at);
  // 4 khoản đầu được bù đủ, các khoản còn lại để chờ hoặc quá hạn theo hạn bù
  if (misuseCase < 5 || (misuseCase % 3 === 0 && dueDate >= NOW_DATE)) {
    const topupTs = ts(addDays(tsDate(t.closed_at), between(1, 5)));
    if (topupTs <= NOW_TS) {
      const top = add('deposit_transaction', { contract_id: c.id, type: 'TOPUP', amount: t.repair_cost, maintenance_request_id: null, due_date: null, confirmed_by: c._staff, created_at: topupTs, note: 'Sinh viên bù lại tiền cọc sau khi bị khấu trừ' });
      audit(c._staff, 'DEPOSIT_TOPUP', 'DEPOSIT_TRANSACTION', top.id, null, { contractId: c.id, amount: t.repair_cost }, null, topupTs);
    }
  }
  misuseCase++;
});

// ----- phí phòng HK1 2026-2027
const invoices = [];
function makeInvoice(o) {
  const inv = add('invoice', {
    code: null, type: o.type, student_id: o.student_id || null, semester_id: o.semester_id || null, room_id: o.room_id || null, billing_month: o.billing_month || null,
    total_amount: o.total, issued_at: o.issued, due_date: o.due, status: o.status, payer_user_id: o.payer || null, payer_name: o.payerName || null,
    payment_method: o.method || null, paid_at: o.paidAt || null, confirmed_by: o.confirmedBy || null, issued_by: o.issuedBy, creation_source: o.source || 'ISSUED',
  });
  invoices.push(inv);
  hist('INVOICE', inv.id, null, 'UNPAID', o.issuedBy, null, o.issued);
  if (o.status === 'PAID') {
    hist('INVOICE', inv.id, 'UNPAID', 'PAID', o.confirmedBy, null, o.paidAt);
    audit(o.confirmedBy, 'INVOICE_CONFIRM_PAYMENT', 'INVOICE', inv.id, { status: 'UNPAID' }, { status: 'PAID', method: o.method }, null, o.paidAt);
  }
  return inv;
}
function roomFeeInvoice(c, assignment, semester, issuedDate, price, kindLabel, statusPlan) {
  const due = semester.room_fee_due_date;
  const issuedTs = ts(issuedDate, 9, 0);
  const dueDate = due >= issuedDate ? due : addDays(issuedDate, 7);
  let status = 'UNPAID';
  let method = null;
  let paidAt = null;
  let confirmedBy = null;
  if (statusPlan === 'PAID') {
    status = 'PAID';
    method = chance(0.6) ? 'TRANSFER' : 'CASH';
    const paidDate = addDays(issuedDate, between(1, Math.max(2, Math.min(25, diffDays(dueDate, issuedDate)))));
    paidAt = ts(paidDate);
    confirmedBy = c._staff || pickStaff(c._unit || 1);
  }
  const inv = makeInvoice({
    type: 'ROOM_FEE', student_id: c.student_id, semester_id: semester.id, total: price, issued: issuedTs, due: dueDate, status, payer: c.student_id,
    payerName: STU.get(c.student_id).name, method, paidAt, confirmedBy, issuedBy: c._staff || pickStaff(c._unit || 1), source: chance(0.2) ? 'MANUAL' : 'ISSUED',
  });
  add('invoice_item', { invoice_id: inv.id, item_type: 'ROOM_FEE', quantity: 1, unit_price: price, amount: price, assignment_id: assignment.id, utility_reading_id: null, adjusts_invoice_id: null, note: kindLabel });
  return inv;
}
activeContracts.forEach((c) => {
  const first = c._assign;
  const plan = chance(0.82) ? 'PAID' : 'UNPAID';
  roomFeeInvoice(c, first, S4, '2026-08-28', first.price_main, 'Phí phòng học kỳ 1 năm học 2026-2027', plan);
});

// ----- hợp đồng lịch sử (HK2 và kỳ hè 2025-2026) của sinh viên cũ và đang ở
const historyStudents = [...byCat('exstudent'), ...activeStudents.filter((s) => has(s, 'history'))];
function randomBedHistory(student, prefer) {
  if (prefer && prefer.status === 'ACTIVE') return prefer;
  const candidates = rooms.filter((r) => r.gender === student.gender).flatMap((r) => r._beds).filter((b) => b.status === 'ACTIVE');
  return pick(candidates);
}
const storageRecords = [];
const histKinds = ['H1', 'H2', 'H3', 'H4'];
const terminatedEx = shuffle(byCat('exstudent')).slice(0, 5);
historyStudents.forEach((s) => {
  const isCurrent = s.cat === 'active';
  const bed = randomBedHistory(s, isCurrent ? roomOf.get(s.id) : null);
  const unit = ROOM_UNIT[bed._room.building_id];
  const staffId = pickStaff(unit);
  const kind = terminatedEx.includes(s) ? 'TERM' : pick(histKinds);
  const sub = `2025-12-${pad(between(12, 28))}`;
  const app = makeApp(s, P2, 'ASSIGNED', { bed, unit, submitted: sub });
  const approvedTs = ts(addDays(sub, 3));
  const createdDate = '2026-01-1' + between(0, 9);
  const createdTs = ts(createdDate);
  advanceApp(app, [['APPROVED', staffId, null, approvedTs], ['ASSIGNED', staffId, null, createdTs]]);
  const endDate = kind === 'H2' || kind === 'H3' ? '2026-08-16' : '2026-06-14';
  const c = makeContract(app, s, '2026-01-26', endDate, 'PENDING', createdTs, { bed, staff: staffId });
  c._unit = unit;
  c._staff = staffId;
  const activatedTs = ts(addDays(createdDate, between(1, 5)));
  const a = makeAssignment(c, bed, createdTs, activatedTs, staffId);
  activate(c, activatedTs, staffId);
  c._assign = a;
  roomFeeInvoice(c, a, S2, '2026-01-28', a.price_main, 'Phí phòng học kỳ 2 năm học 2025-2026', 'PAID');
  let endTs;
  let endReason = 'MOVE_OUT';
  let finalStatus = 'EXPIRED';
  if (kind === 'TERM') {
    const termDate = `2026-0${between(3, 5)}-${pad(between(2, 26))}`;
    endTs = ts(termDate);
    finalStatus = 'TERMINATED';
    c.end_date = endDate;
    c.status_reason = pick(['Sinh viên xin trả phòng trước hạn vì chuyển đi thực tập dài hạn.', 'Sinh viên vi phạm nội quy khu và bị chấm dứt hợp đồng.', 'Sinh viên nghỉ học tạm thời, xin chấm dứt hợp đồng.']);
  } else if (kind === 'H1' || kind === 'H4') endTs = ts('2026-06-14', 10, 0);
  else endTs = ts('2026-08-16', 10, 0);
  if (kind === 'H3') {
    // rời tạm thời mùa hè: giữ hợp đồng, trả giường
    endTs = ts('2026-06-20', 9, 0);
    endReason = 'SUMMER_LEAVE';
  }
  closeAssignment(a, endTs, endReason);
  c.status = finalStatus;
  c.closed_at = endTs;
  hist('CONTRACT', c.id, 'ACTIVE', finalStatus, finalStatus === 'TERMINATED' ? staffId : null, c.status_reason, endTs);
  // kỳ hè 2026
  if (kind === 'H2') {
    const ext = add('contract_extension_request', {
      contract_id: c.id, source: 'SUMMER_STAY', old_end_date: '2026-06-14', new_end_date: '2026-08-16', reason: 'Gia hạn để ở lại trong kỳ hè theo lựa chọn đã được duyệt.',
      status: 'APPROVED', decided_by: staffId, decided_at: ts('2026-05-12'), decision_reason: null, created_at: ts('2026-05-08'),
    });
    hist('EXTENSION', ext.id, null, 'PENDING', s.id, null, ext.created_at);
    hist('EXTENSION', ext.id, 'PENDING', 'APPROVED', staffId, null, ext.decided_at);
    const sc = makeSummerChoice(s, 'STAY', 'APPROVED', S3, '2026-05-08', { contract: c, decider: staffId });
    summerChoices.push(sc);
    roomFeeInvoice(c, a, S3, '2026-06-25', a.price_summer, 'Phí phòng kỳ hè năm học 2025-2026', chance(0.9) ? 'PAID' : 'UNPAID');
  } else if (kind === 'H3') {
    const sc = makeSummerChoice(s, 'TEMP_LEAVE', 'APPROVED', S3, '2026-05-10', { contract: c, decider: staffId });
    summerChoices.push(sc);
    const loc = T.storage_location.find((l) => l.management_unit_id === unit && l.building_id === bed._room.building_id) || T.storage_location[0];
    const deposited = '2026-06-19';
    const returned = chance(0.8);
    const sr = add('storage_record', {
      contract_id: c.id, summer_choice_id: sc.id, previous_bed_id: bed.id, package_count: between(1, 5),
      description: pick(['Vali quần áo và 2 thùng sách', 'Chăn màn, gối và 3 thùng đồ dùng cá nhân', 'Xe đạp gấp và 1 thùng carton', 'Quạt, ấm điện và 2 túi quần áo']),
      deposited_at: deposited, received_by: staffId, returned_at: returned ? addDays('2026-08-10', between(0, 5)) : null, returned_by: returned ? staffId : null, location_id: loc.id,
    }, true);
    storageRecords.push(sr);
    audit(staffId, 'STORAGE_CREATE', 'STORAGE', sr.id, null, { contractId: c.id, packages: sr.package_count }, null, ts(deposited));
    if (returned) audit(staffId, 'STORAGE_RETURN', 'STORAGE', sr.id, null, { returnedAt: sr.returned_at }, null, ts(sr.returned_at));
  } else if (kind === 'H4') {
    summerChoices.push(makeSummerChoice(s, 'LEAVE', 'APPROVED', S3, '2026-05-11', { contract: c, decider: staffId }));
  }
  if (kind === 'H3') {
    c.closed_at = ts('2026-08-16', 10, 0);
    c._moveOutTs = c.closed_at;
  } else c._moveOutTs = endTs;
  c._hist = true;
  c._kind = kind;
});

// ----- hợp đồng chấm dứt giữa HK1 2026-2027 (6 sinh viên trả phòng sớm)
terminatedList.forEach((s, i) => {
  const bed = freeBedAnywhere(s.gender);
  const unit = ROOM_UNIT[bed._room.building_id];
  const staffId = pickStaff(unit);
  const sub = `2026-07-${pad(between(5, 25))}`;
  const app = makeApp(s, P4, 'ASSIGNED', { bed, unit, submitted: sub });
  const approvedTs = ts(addDays(sub, 3));
  const createdDate = `2026-08-0${between(3, 9)}`;
  const createdTs = ts(createdDate);
  advanceApp(app, [['APPROVED', staffId, null, approvedTs], ['ASSIGNED', staffId, null, createdTs]]);
  const c = makeContract(app, s, '2026-08-24', '2027-01-17', 'PENDING', createdTs, { bed, staff: staffId });
  c._unit = unit;
  c._staff = staffId;
  const activatedTs = ts(addDays(createdDate, 2));
  const a = makeAssignment(c, bed, createdTs, activatedTs, staffId);
  activate(c, activatedTs, staffId);
  c._assign = a;
  roomFeeInvoice(c, a, S4, '2026-08-28', a.price_main, 'Phí phòng học kỳ 1 năm học 2026-2027', i === 0 ? 'UNPAID' : 'PAID');
  const termDate = `2026-1${between(0, 1)}-${pad(between(1, 18))}`;
  const endTs = ts(termDate);
  closeAssignment(a, endTs, 'MOVE_OUT');
  c.status = 'TERMINATED';
  c.closed_at = endTs;
  c.status_reason = pick(['Sinh viên xin trả phòng để thuê nhà ở ngoài.', 'Sinh viên bảo lưu kết quả học tập, trả phòng trước hạn.', 'Sinh viên chuyển sang cơ sở thực tập ở tỉnh khác.']);
  hist('CONTRACT', c.id, 'ACTIVE', 'TERMINATED', staffId, c.status_reason, endTs);
  c._hist = true;
  c._moveOutTs = endTs;
  c._kind = 'TERM';
});

// ----- trả phòng: biên bản và hoàn cọc
const histContracts = contracts.filter((c) => c._hist);
histContracts.forEach((c, i) => {
  const dep = c._building.deposit;
  let extraDeduct = 0;
  const damaged = chance(0.18) || (i === 0);
  let unpaid = 0;
  const unpaidInvoice = invoices.find((v) => v.student_id === c.student_id && v.status === 'UNPAID' && v.type === 'ROOM_FEE');
  if (unpaidInvoice && c.status !== 'ACTIVE') unpaid = unpaidInvoice.total_amount;
  if (damaged && i % 5 === 0) {
    // khoản trừ cọc lớn hơn cọc để tạo công nợ cọc
    const t = makeTicket({ room: c._bed._room, requester: c.student_id, category: 'FURNITURE', severity: 'MEDIUM', description: 'Giường và tủ cá nhân bị hư hỏng nặng khi trả phòng.', status: 'CLOSED', created: ts(addDays(tsDate(c._moveOutTs), -4)), assigned: pickTech(c._unit), resolvedAt: ts(addDays(tsDate(c._moveOutTs), -2)), closedAt: ts(addDays(tsDate(c._moveOutTs), -1)), cause: 'MISUSE', cost: dep + 500000, costNote: 'Hư hỏng nặng do sử dụng sai cách khi trả phòng', costBy: c._staff, costAt: ts(addDays(tsDate(c._moveOutTs), -1)) });
    t._unit = c._unit;
    hist('MAINTENANCE', t.id, null, 'OPEN', c._staff, null, t.created_at);
    hist('MAINTENANCE', t.id, 'OPEN', 'ASSIGNED', c._staff, null, t.created_at);
    hist('MAINTENANCE', t.id, 'ASSIGNED', 'IN_PROGRESS', t.assigned_to, null, t.created_at);
    hist('MAINTENANCE', t.id, 'IN_PROGRESS', 'RESOLVED', t.assigned_to, null, t.resolved_at);
    hist('MAINTENANCE', t.id, 'RESOLVED', 'CLOSED', c._staff, null, t.closed_at);
    const ded = add('deposit_transaction', { contract_id: c.id, type: 'DEDUCTION', amount: -(dep + 500000), maintenance_request_id: t.id, due_date: addDays(tsDate(t.closed_at), 7), confirmed_by: null, created_at: t.closed_at, note: 'Khấu trừ chi phí sửa chữa nội thất khi trả phòng' });
    deductions.push({ ...ded, _ticket: t });
    ded._ticket = t;
    extraDeduct = dep + 500000;
  }
  const balance = dep - extraDeduct;
  const refund = Math.max(balance, 0);
  if (refund > 0) {
    const rf = add('deposit_transaction', { contract_id: c.id, type: 'REFUND', amount: -refund, maintenance_request_id: null, due_date: null, confirmed_by: c._staff, created_at: addDays(tsDate(c._moveOutTs), 0) + ' 15:00:00+07', note: 'Hoàn tiền cọc khi trả phòng' });
    audit(c._staff, 'DEPOSIT_REFUND', 'DEPOSIT_TRANSACTION', rf.id, null, { contractId: c.id, amount: -refund }, null, rf.created_at);
  }
  const rec = add('move_out_record', {
    contract_id: c.id, moved_out_at: c._moveOutTs, asset_condition: damaged ? 'DAMAGED' : 'OK',
    asset_note: damaged ? pick(['Hỏng một ngăn tủ cá nhân và khóa tủ.', 'Giường tầng bị cong thanh ngang, cần hàn lại.', 'Cửa sổ vỡ kính, đã lập biên bản.']) : null,
    unpaid_room_fee: unpaid, deposit_balance: balance, refund_amount: refund, confirmed_by: c._staff,
  });
  moveOuts.push(rec);
  audit(c._staff, 'CONTRACT_MOVE_OUT', 'MOVE_OUT', rec.id, { status: 'ACTIVE' }, { status: c.status, refund }, null, c._moveOutTs);
});

// ---------------------------------------------------------------- điện nước
const RATES = [
  ['ELECTRIC', 3000, '2025-01-01'], ['ELECTRIC', 3300, '2025-09-01'], ['ELECTRIC', 3500, '2026-07-01'], ['ELECTRIC', 3700, '2027-01-01'],
  ['WATER', 15000, '2025-01-01'], ['WATER', 17000, '2026-07-01'], ['WATER', 18500, '2027-03-01'],
];
RATES.forEach(([type, price, from]) => {
  const r = add('utility_rate', { meter_type: type, unit_price: price, effective_from: from, created_by: ADMIN });
  audit(ADMIN, 'RATE_CREATE', 'UTILITY_RATE', r.id, null, { type, price, effectiveFrom: from }, null, ts(addDays(from, -10)));
});
const rateAt = (type, date) => T.utility_rate.filter((r) => r.meter_type === type && r.effective_from <= date).sort((a, b) => (a.effective_from < b.effective_from ? 1 : -1))[0];
const MONTHS = ['2026-06-01', '2026-07-01', '2026-08-01', '2026-09-01', '2026-10-01'];
const occCount = (room) => room._beds.filter((b) => occupant.has(b.id)).length;
const readings = {};
const meterReplace = new Set(shuffle(occupiedRooms).slice(0, 3).map((r) => r.id));
rooms.filter((r) => r.status === 'ACTIVE').forEach((room) => {
  const unit = ROOM_UNIT[room.building_id];
  let elecStart = between(800, 4500);
  let waterStart = between(120, 900);
  MONTHS.forEach((month, mi) => {
    const occ = occCount(room);
    const factor = [0.3, 0.3, 0.6, 1, 1][mi];
    const people = Math.round(occ * factor);
    const eCons = people === 0 ? between(0, 9) : people * between(26, 52) + between(10, 40);
    const wCons = people === 0 ? between(0, 2) : people * between(2, 5) + between(0, 3);
    const rec = (t, start, cons, replace) => {
      let row;
      if (replace) {
        const part = Math.floor(cons * 0.6);
        const newStart = between(0, 12);
        row = { room_id: room.id, meter_type: t, billing_month: month, start_reading: start, end_reading: newStart + (cons - part), old_meter_final: start + part, new_meter_start: newStart, replacement_reason: 'Thay công tơ điện do hỏng màn hình hiển thị', recorded_by: pickStaff(unit), recorded_at: ts(addDays(month, 31 + 0)) };
      } else {
        row = { room_id: room.id, meter_type: t, billing_month: month, start_reading: start, end_reading: start + cons, old_meter_final: null, new_meter_start: null, replacement_reason: null, recorded_by: pickStaff(unit), recorded_at: ts(addDays(month, 29)) };
      }
      row._cons = replace ? (row.old_meter_final - row.start_reading) + (row.end_reading - row.new_meter_start) : row.end_reading - row.start_reading;
      return add('utility_reading', row);
    };
    const replace = meterReplace.has(room.id) && mi === 3;
    const er = rec('ELECTRIC', elecStart, eCons, replace);
    const wr = rec('WATER', waterStart, wCons, false);
    elecStart = er.end_reading;
    waterStart = wr.end_reading;
    (readings[room.id] = readings[room.id] || {})[month] = { e: er, w: wr };
    audit(er.recorded_by, 'READING_CREATE', 'ROOM', room.id, null, { month, meter: 'ELECTRIC', consumption: eCons }, null, er.recorded_at);
  });
});

// hóa đơn điện nước tháng 8–10/2026
const utilInvoices = [];
function leaderOrAny(room) {
  if (room.leader_student_id) return STU.get(room.leader_student_id);
  const list = room._beds.filter((b) => occupant.has(b.id)).map((b) => occupant.get(b.id)).filter((x) => contractOf.has(x.id));
  return list.length ? list[0] : null;
}
// trưởng phòng: mỗi phòng có người ở và đang ACTIVE chọn một người đại diện
occupiedRooms.forEach((room) => {
  const list = room._beds.filter((b) => occupant.has(b.id)).map((b) => occupant.get(b.id)).filter((s) => contractOf.has(s.id));
  if (list.length >= 2 && chance(0.82)) room.leader_student_id = pick(list).id;
});
const skipOct = new Set(shuffle(occupiedRooms).slice(0, 6).map((r) => r.id));
occupiedRooms.forEach((room) => {
  const payer = leaderOrAny(room);
  if (!payer) return;
  const unit = ROOM_UNIT[room.building_id];
  ['2026-08-01', '2026-09-01', '2026-10-01'].forEach((month, mi) => {
    if (month === '2026-10-01' && skipOct.has(room.id)) return;
    const rd = readings[room.id][month];
    const eRate = rateAt('ELECTRIC', month);
    const wRate = rateAt('WATER', month);
    const eAmt = rd.e._cons * eRate.unit_price;
    const wAmt = rd.w._cons * wRate.unit_price;
    const issueDate = month === '2026-08-01' ? '2026-09-04' : month === '2026-09-01' ? '2026-10-05' : (chance(0.22) ? '2026-11-18' : '2026-11-04');
    const issuedBy = pickStaff(unit);
    const due = addDays(issueDate, 7);
    let status = 'UNPAID';
    let method = null;
    let paidAt = null;
    let confirmedBy = null;
    const payChance = month === '2026-10-01' ? (issueDate === '2026-11-18' ? 0.15 : 0.55) : 0.93;
    if (chance(payChance)) {
      status = 'PAID';
      method = chance(0.55) ? 'TRANSFER' : 'CASH';
      paidAt = ts(addDays(issueDate, between(1, 6)));
      confirmedBy = pickStaff(unit);
    }
    const inv = makeInvoice({ type: 'UTILITY', room_id: room.id, billing_month: month, total: eAmt + wAmt, issued: ts(issueDate, 9, 0), due, status, payer: payer.id, payerName: payer.name, method, paidAt, confirmedBy, issuedBy, source: chance(0.1) ? 'MANUAL' : 'ISSUED' });
    add('invoice_item', { invoice_id: inv.id, item_type: 'ELECTRIC', quantity: rd.e._cons, unit_price: eRate.unit_price, amount: eAmt, assignment_id: null, utility_reading_id: rd.e.id, adjusts_invoice_id: null, note: null });
    add('invoice_item', { invoice_id: inv.id, item_type: 'WATER', quantity: rd.w._cons, unit_price: wRate.unit_price, amount: wAmt, assignment_id: null, utility_reading_id: rd.w.id, adjusts_invoice_id: null, note: null });
    inv._room = room;
    inv._month = month;
    utilInvoices.push(inv);
  });
});
// điều chỉnh hóa đơn: hóa đơn tháng 10 điều chỉnh cho tháng 9 của cùng phòng
shuffle(utilInvoices.filter((v) => v._month === '2026-10-01')).slice(0, 3).forEach((oct) => {
  const sep = utilInvoices.find((v) => v._month === '2026-09-01' && v._room.id === oct._room.id);
  if (!sep) return;
  const adj = -Math.min(60000, Math.floor(oct.total_amount / 2));
  add('invoice_item', { invoice_id: oct.id, item_type: 'ADJUSTMENT', quantity: 1, unit_price: adj, amount: adj, assignment_id: null, utility_reading_id: null, adjusts_invoice_id: sep.id, note: 'Điều chỉnh giảm do ghi nhầm chỉ số công tơ nước tháng 9' });
  oct.total_amount += adj;
});

// ---------------------------------------------------------------- thông báo
const notifications = [];
function notify(row, readers = []) {
  const n = add('notification', { management_unit_id: null, building_id: null, floor: null, room_id: null, user_id: null, source_type: null, source_id: null, created_by: null, ...row });
  notifications.push(n);
  readers.forEach((u) => add('notification_read', { notification_id: n.id, user_id: u, read_at: ts(addDays(tsDate(n.created_at), Math.min(between(0, 3), diffDays(NOW_DATE, tsDate(n.created_at))))) }, false));
  return n;
}
const allStudentIds = students.map((s) => s.id);
const someStudents = (p) => allStudentIds.filter(() => chance(p));
notify({ scope: 'ALL', title: 'Mở đăng ký ở học kỳ 2 năm học 2026-2027', body: 'Đợt đăng ký ở học kỳ 2 năm học 2026-2027 mở từ 08:00 ngày 16/11/2026 đến 23:59 ngày 08/12/2026. Sinh viên đăng nhập hệ thống, hoàn tất đơn và nộp minh chứng diện ưu tiên (nếu có) trong thời hạn trên.', source_type: 'MANUAL', created_by: ADMIN, created_at: ts('2026-11-14', 9, 0) }, someStudents(0.5));
notify({ scope: 'ALL', title: 'Mở đăng ký ở và gửi đồ kỳ hè 2027', body: 'Sinh viên có nhu cầu ở lại, tạm rời hoặc rời ký túc xá trong kỳ hè 2027 đăng ký lựa chọn trong hệ thống từ 09/11/2026 đến 20/12/2026.', source_type: 'SUMMER_PERIOD_OPENED', created_by: null, created_at: ts('2026-11-09', 8, 0) }, someStudents(0.45));
notify({ scope: 'ALL', title: 'Thông báo về quy định phòng cháy chữa cháy', body: 'Nghiêm cấm sử dụng bếp điện, bếp gas mini và các thiết bị đun nấu công suất lớn trong phòng ở. Cán bộ khu sẽ kiểm tra đột xuất; các trường hợp vi phạm bị lập biên bản và xử lý theo nội quy.', source_type: 'MANUAL', created_by: ADMIN, created_at: ts('2026-10-20', 14, 0) }, someStudents(0.6));
notify({ scope: 'ALL', title: 'Lịch nghỉ lễ và giờ đóng cửa ký túc xá dịp 20/11', body: 'Ký túc xá đóng cổng lúc 23:30 hằng ngày. Dịp nghỉ lễ 20/11, sinh viên về quê báo trước với cán bộ quản lý tầng để thuận tiện kiểm soát ra vào.', source_type: 'MANUAL', created_by: ADMIN, created_at: ts('2026-11-17', 10, 0) }, someStudents(0.4));
notify({ scope: 'UNIT', management_unit_id: 1, title: 'Họp sinh viên khu K1', body: 'Khu K1 tổ chức họp sinh viên lúc 19:00 thứ Năm ngày 26/11/2026 tại phòng sinh hoạt chung tầng trệt để phổ biến nội quy và kế hoạch kiểm tra vệ sinh.', source_type: 'MANUAL', created_by: staff[1][0], created_at: ts('2026-11-20', 15, 0) }, someStudents(0.2));
notify({ scope: 'UNIT', management_unit_id: 2, title: 'Kiểm tra an toàn điện toàn khu N2 + N3', body: 'Tổ kỹ thuật kiểm tra hệ thống điện và thiết bị an toàn từ 08:00 đến 11:30 ngày 27/11/2026. Đề nghị sinh viên có mặt hoặc để lại thông tin liên hệ khi vắng.', source_type: 'MANUAL', created_by: staff[2][0], created_at: ts('2026-11-21', 11, 0) }, someStudents(0.2));
notify({ scope: 'BUILDING', building_id: 2, title: 'Cắt điện bảo trì tòa N2', body: 'Tòa N2 cắt điện từ 08:00 đến 11:00 ngày 25/11/2026 để bảo trì trạm biến áp. Đề nghị sinh viên sạc đầy thiết bị trước đó.', source_type: 'MANUAL', created_by: staff[2][0], created_at: ts('2026-11-21', 16, 0) }, someStudents(0.15));
notify({ scope: 'BUILDING', building_id: 3, title: 'Vệ sinh bể nước tòa N3', body: 'Tòa N3 tạm ngưng cấp nước từ 13:00 đến 17:00 ngày 24/11/2026 để vệ sinh bể chứa. Sinh viên chủ động dự trữ nước sinh hoạt.', source_type: 'MANUAL', created_by: staff[2][1], created_at: ts('2026-11-20', 9, 30) }, someStudents(0.15));
notify({ scope: 'FLOOR', building_id: 1, floor: 4, title: 'Kiểm tra nội vụ tầng 4 tòa K1', body: 'Cán bộ khu kiểm tra nội vụ và vệ sinh các phòng tầng 4 vào sáng ngày 28/11/2026. Sinh viên giữ phòng sạch sẽ, không để vật dụng ngoài hành lang.', source_type: 'MANUAL', created_by: staff[1][1], created_at: ts('2026-11-21', 14, 0) }, someStudents(0.1));
notify({ scope: 'FLOOR', building_id: 2, floor: 2, title: 'Sửa chữa hệ thống nước tầng 2 tòa N2', body: 'Tổ kỹ thuật thay đường ống nước khu vệ sinh tầng 2 trong ngày 23/11/2026; nước có thể bị gián đoạn từng đợt ngắn.', source_type: 'MANUAL', created_by: staff[2][2], created_at: ts('2026-11-21', 17, 0) }, someStudents(0.1));
const roomSample = shuffle(occupiedRooms).slice(0, 8);
roomSample.forEach((room, i) =>
  notify({ scope: 'ROOM', room_id: room.id, title: ['Nhắc nhở vệ sinh phòng ở', 'Thông báo kiểm kê tài sản trong phòng', 'Nhắc thanh toán hóa đơn điện nước', 'Lịch sửa điều hòa trong phòng'][i % 4], body: ['Phòng bị nhắc nhở lần 1 do để rác quá giờ quy định. Đề nghị các thành viên thực hiện đổ rác trước 21:00 hằng ngày.', 'Cán bộ tầng sẽ kiểm kê tài sản trong phòng vào chiều mai. Các thành viên cần có mặt để xác nhận tình trạng.', 'Hóa đơn điện nước tháng 10 của phòng đã quá hạn. Đề nghị trưởng phòng đóng tiền sớm để tránh bị nhắc nhở.', 'Tổ kỹ thuật sẽ vào bảo dưỡng điều hòa trong phòng vào ngày 24/11. Vui lòng bố trí người có mặt.'][i % 4], source_type: i % 4 === 2 ? 'INVOICE_ISSUED' : 'MANUAL', created_by: pickStaff(ROOM_UNIT[room.building_id]), created_at: ts(`2026-11-${pad(between(10, 21))}`) }, room._beds.filter((b) => occupant.has(b.id)).map((b) => occupant.get(b.id).id).filter(() => chance(0.5))),
);
// thông báo cá nhân sinh ra từ nghiệp vụ
const personal = (user, title, body, source, sourceId, created, read) => notify({ scope: 'USER', user_id: user, title, body, source_type: source, source_id: sourceId, created_by: null, created_at: created }, read ? [user] : []);
apps.filter((a) => a.status === 'APPROVED' || a.status === 'ASSIGNED').forEach((a) => {
  personal(a.student_id, 'Đơn đăng ký ở đã được duyệt', 'Đơn đăng ký ở của bạn đã được duyệt. Vui lòng theo dõi thông báo tiếp theo về giường được giữ và hạn kích hoạt hợp đồng.', 'APPLICATION_APPROVED', a.id, ts(addDays(tsDate(a.submitted_at), 3)), chance(0.8));
});
apps.filter((a) => a.status === 'REJECTED').forEach((a) => {
  personal(a.student_id, 'Đơn đăng ký ở bị từ chối', `Đơn đăng ký ở của bạn bị từ chối. Lý do: ${a.status_reason}`, 'APPLICATION_REJECTED', a.id, ts(addDays(tsDate(a.submitted_at), 3)), chance(0.6));
});
pendingContracts.forEach((c) => {
  personal(c.student_id, 'Giường đã được giữ cho bạn', `Giường ${c._bed.code} phòng ${c._bed._room.code} đã được giữ. Hãy đến văn phòng khu nộp tiền đặt cọc ${vnd(c._building.deposit)} đồng để kích hoạt hợp đồng trước ${c.activation_deadline.slice(0, 10)}.`, 'BED_HELD', c.id, c.created_at, chance(0.4));
});
tickets.filter((t) => t.status !== 'OPEN').forEach((t, i) => {
  if (i % 2) return;
  personal(t.requester_id, `Phiếu sửa chữa ${'{code}'} cập nhật trạng thái`, `Phiếu sửa chữa của bạn đã chuyển sang trạng thái ${t.status}.`, 'TICKET_STATUS_CHANGED', t.id, t.resolved_at || t.closed_at || t.created_at, chance(0.7));
  notifications[notifications.length - 1]._ticket = t;
});
deductions.forEach((d) => {
  const c = contracts.find((x) => x.id === d.contract_id);
  personal(c.student_id, 'Tiền cọc bị khấu trừ', `Tiền cọc của bạn bị khấu trừ ${vnd(Math.abs(d.amount))} đồng cho chi phí sửa chữa do sử dụng sai cách. Hạn bù cọc: ${d.due_date}.`, 'DEPOSIT_DEDUCTED', d.id, d.created_at, chance(0.6));
});
activeContracts.filter((c) => diffDays(c.end_date, NOW_DATE) <= 15 && diffDays(c.end_date, NOW_DATE) >= 0).forEach((c) => {
  personal(c.student_id, 'Hợp đồng sắp hết hạn', `Hợp đồng ${'{code}'} của bạn hết hạn ngày ${c.end_date}. Hãy gửi yêu cầu gia hạn hoặc chuẩn bị trả phòng đúng hạn.`, 'CONTRACT_EXPIRING', c.id, ts(addDays(NOW_DATE, -1), 8, 0), chance(0.3));
  notifications[notifications.length - 1]._contract = c;
});
utilInvoices.filter((v) => v._month === '2026-10-01').forEach((v) => {
  personal(v.payer_user_id, 'Hóa đơn điện nước tháng 10/2026', `Hóa đơn điện nước phòng ${v._room.code} tháng 10/2026 là ${vnd(v.total_amount)} đồng, hạn đóng ${v.due_date}.`, 'INVOICE_ISSUED', v.id, v.issued_at, v.status === 'PAID' || chance(0.4));
});
moveOuts.forEach((m) => {
  const c = contracts.find((x) => x.id === m.contract_id);
  personal(c.student_id, 'Hoàn tất thủ tục trả phòng', `Hợp đồng đã được chốt trả phòng. Tiền cọc được hoàn: ${vnd(m.refund_amount)} đồng.`, 'CONTRACT_MOVED_OUT', m.id, m.moved_out_at, true);
});
storageRecords.forEach((sr) => {
  const c = contracts.find((x) => x.id === sr.contract_id);
  personal(c.student_id, 'Cập nhật hồ sơ gửi đồ', sr.returned_at ? 'Đồ gửi của bạn đã được trả. Cảm ơn bạn đã sử dụng dịch vụ.' : 'Đồ gửi của bạn vẫn đang được lưu kho. Vui lòng đến nhận sớm để giải phóng chỗ.', 'STORAGE_RECORD_CHANGED', sr.id, ts(sr.returned_at || addDays(sr.deposited_at, 1)), chance(0.5));
});

// ---------------------------------------------------------------- ghi chú audit hệ thống
audit(ADMIN, 'CONFIG_UPDATE', 'CATALOG', null, { key: 'contract.activation_days', value: '5' }, { key: 'contract.activation_days', value: '7' }, 'Kéo dài hạn kích hoạt hợp đồng theo quy chế mới', ts('2026-06-18'));
audit(ADMIN, 'ROLE_PERMISSION_UPDATE', 'USER', null, null, { role: 'STAFF', changed: 'SUMMER_REGISTRATION_UPDATE' }, 'Cấp quyền xét duyệt lựa chọn kỳ hè cho cán bộ khu', ts('2026-11-10'));
audit(ADMIN, 'STUDENT_IMPORT', 'USER', null, null, { rows: 112, created: 112, source: 'danh-sach-sinh-vien-K67-nam-2025.xlsx' }, null, ts('2025-08-26'));
audit(ADMIN, 'STUDENT_IMPORT', 'USER', null, null, { rows: 94, created: 94, source: 'danh-sach-sinh-vien-K68-nam-2026.xlsx' }, null, ts('2026-07-22'));
T.app_user.filter((u) => u.role === 'STAFF' || u.role === 'TECHNICIAN').forEach((u) => audit(ADMIN, 'USER_CREATE', 'USER', u.id, null, { username: u.username, role: u.role }, null, u.created_at));
T.app_user.filter((u) => u.locked && u.role !== 'STUDENT').forEach((u) => audit(ADMIN, 'USER_LOCK', 'USER', u.id, { locked: false }, { locked: true }, 'Cán bộ đã nghỉ việc, thu hồi quyền truy cập', ts('2026-09-05')));
T.app_user.filter((u) => u.locked && u.role === 'STUDENT').forEach((u) => audit(ADMIN, 'USER_LOCK', 'USER', u.id, { locked: false }, { locked: true }, 'Nhập sai mật khẩu quá số lần quy định và bị khóa', ts('2026-10-12')));
T.registration_period.forEach((p) => audit(p.created_by, 'REGISTRATION_PERIOD_CREATE', 'REGISTRATION_PERIOD', p.id, null, { name: p.name, kind: p.kind }, null, ts(addDays(p.open_at.slice(0, 10), -14))));
rooms.slice(0, 20).forEach((r) => audit(ADMIN, 'ROOM_CREATE', 'ROOM', r.id, null, { code: r.code, bedCount: r._beds.length }, null, ts('2025-06-12')));
Object.entries(SPECIAL_ROOMS).forEach(([code, [status, reason]]) => {
  const r = rooms.find((x) => x.code === code);
  audit(pickStaff(ROOM_UNIT[r.building_id]), 'ROOM_STATUS', 'ROOM', r.id, { status: 'ACTIVE' }, { status }, reason, ts('2026-10-' + pad(between(2, 20))));
});
rooms.filter((r) => r.leader_student_id).slice(0, 15).forEach((r) => audit(pickStaff(ROOM_UNIT[r.building_id]), 'ROOM_LEADER_CHANGE', 'ROOM', r.id, null, r.leader_student_id, null, ts('2026-09-' + pad(between(1, 28)))));
T.bed.filter((b) => b.status !== 'ACTIVE').forEach((b) => audit(pickStaff(ROOM_UNIT[ROOM_BY_ID.get(b.room_id).building_id]), 'BED_STATUS', 'BED', b.id, { status: 'ACTIVE' }, { status: b.status }, b.note, ts('2026-10-' + pad(between(1, 25)))));

// ---------------------------------------------------------------- hoàn tất mã, ghi đè phụ thuộc
function numberCodes(list, assign) {
  list.sort((a, b) => (a._sort < b._sort ? -1 : a._sort > b._sort ? 1 : 0));
  list.forEach(assign);
}
// hợp đồng: HD-<năm tạo>-<số>
const contractList = T.dormitory_contract.slice().sort((a, b) => (a.created_at < b.created_at ? -1 : 1));
contractList.forEach((c, i) => {
  c.code = `HD-${c.created_at.slice(0, 4)}-${pad(i + 1, 4)}`;
});
const ticketList = T.maintenance_request.slice().sort((a, b) => (a.created_at < b.created_at ? -1 : 1));
ticketList.forEach((t, i) => {
  t.code = `TK-${pad(i + 1, 4)}`;
});
const invoiceList = T.invoice.slice().sort((a, b) => (a.issued_at < b.issued_at ? -1 : 1));
invoiceList.forEach((v, i) => {
  v.code = `${v.type === 'UTILITY' ? 'DN' : 'HP'}-${v.issued_at.slice(0, 4)}-${pad(i + 1, 4)}`;
});
// thay chỗ {code} trong nội dung
T.deposit_transaction.forEach((d) => {
  if (d._ticket) d.note = d.note.replace('{code}', d._ticket.code);
});
notifications.forEach((n) => {
  if (n._ticket) n.title = n.title.replace('{code}', n._ticket.code);
  if (n._contract) n.body = n.body.replace('{code}', n._contract.code);
});
T.deposit_transaction.forEach((d) => delete d._ticket);

// user_preference
T.app_user.filter((u) => u.role !== 'STUDENT' || chance(0.1)).forEach((u) =>
  add('user_preference', { user_id: u.id, theme_mode: chance(0.2) ? 'dark' : 'light', theme_preset: pick(['blue', 'indigo', 'teal', 'emerald']), table_dense: chance(0.3), version: between(0, 3), updated_at: ts('2026-10-' + pad(between(1, 28))) }, false),
);

// nhật ký audit & lịch sử sắp theo thời gian
AUDIT.sort((a, b) => (a.created_at < b.created_at ? -1 : a.created_at > b.created_at ? 1 : 0));
AUDIT.forEach((row) => add('audit_log', row));
HIST.sort((a, b) => (a.created_at < b.created_at ? -1 : a.created_at > b.created_at ? 1 : 0));
HIST.forEach((row) => add('status_history', row));

// ---------------------------------------------------------------- kiểm tra nội bộ trước khi xuất
function assertTrue(cond, message) {
  if (!cond) throw new Error(`Kiểm tra dữ liệu thất bại: ${message}`);
}
assertTrue(new Set(assignments.filter((a) => !a.ended_at).map((a) => a.bed_id)).size === assignments.filter((a) => !a.ended_at).length, 'trùng giường đang mở');
assertTrue(new Set(assignments.filter((a) => !a.ended_at).map((a) => a.student_id)).size === assignments.filter((a) => !a.ended_at).length, 'trùng sinh viên đang mở');
assertTrue(T.dormitory_contract.filter((c) => ['PENDING', 'ACTIVE'].includes(c.status)).length === new Set(T.dormitory_contract.filter((c) => ['PENDING', 'ACTIVE'].includes(c.status)).map((c) => c.student_id)).size, 'trùng hợp đồng đang hiệu lực');

// ---------------------------------------------------------------- xuất SQL
const ORDER = [
  ['academic_year', true], ['school', true], ['faculty', true], ['student_cohort', true], ['hometown', true], ['amenity', true], ['maintenance_category', true], ['priority_category', true],
  ['management_unit', true], ['app_user', true], ['student_profile', false], ['building', true], ['building_floor', true], ['room_type', true], ['room_type_amenity', false], ['room', true], ['bed', true],
  ['storage_location', true], ['semester', true], ['registration_period', true], ['registration_period_unit', false], ['registration_quota', false], ['room_application', true],
  ['application_priority', false], ['dormitory_contract', true], ['room_assignment_history', true], ['contract_extension_request', true], ['room_transfer_request', true],
  ['summer_choice', true], ['storage_record', true], ['move_out_record', true], ['maintenance_request', true], ['deposit_transaction', true], ['utility_rate', true], ['utility_reading', true],
  ['invoice', true], ['invoice_item', true], ['notification', true], ['notification_read', false], ['status_history', true], ['audit_log', true], ['user_preference', false],
];
const out = [];
out.push(`-- Sinh bởi gen-full-data.cjs; mốc ${NOW_DATE}. Chạy: psql -v ON_ERROR_STOP=1 -f full-data.sql`);
out.push('BEGIN;');
out.push("SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';");
out.push('SET LOCAL lock_timeout = \'20s\';');
out.push(`DO $guard$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '19' AND success) THEN RAISE EXCEPTION 'Cần schema Flyway V19'; END IF;
  IF (SELECT count(*) FROM app_user WHERE username = 'admin' AND role = 'ADMIN') <> 1 THEN RAISE EXCEPTION 'Không tìm thấy đúng một tài khoản admin để giữ lại'; END IF;
END $guard$;`);
out.push('CREATE TEMP TABLE keep_admin ON COMMIT DROP AS SELECT * FROM app_user WHERE username = \'admin\' AND role = \'ADMIN\';');
out.push("CREATE TEMP TABLE keep_kc ON COMMIT DROP AS SELECT username, keycloak_user_id FROM app_user WHERE keycloak_user_id IS NOT NULL AND username <> 'admin';");
out.push('CREATE TEMP TABLE keep_rpm ON COMMIT DROP AS SELECT * FROM role_permission_matrix;');
out.push('CREATE TEMP TABLE keep_cfg ON COMMIT DROP AS SELECT * FROM system_config;');
const ALL_TABLES = ORDER.map((o) => o[0]).concat(['maintenance_photo', 'role_permission_matrix', 'system_config']);
out.push(`TRUNCATE TABLE ${ALL_TABLES.join(', ')} RESTART IDENTITY;`);
['seq_contract_code', 'seq_invoice_code', 'seq_maintenance_code'].forEach((s) => out.push(`ALTER SEQUENCE ${s} RESTART WITH 1;`));
out.push("INSERT INTO system_config(key, value, description, updated_by, updated_at) SELECT key, value, description, NULL, updated_at FROM keep_cfg;");
out.push("INSERT INTO role_permission_matrix(role, permissions, version, updated_by, updated_at) SELECT role, permissions, version, NULL, updated_at FROM keep_rpm;");
function emit(table, hasId) {
  let rows = T[table] || [];
  if (table === 'app_user') rows = rows.filter((r) => r.username);
  if (!rows.length) return;
  const cols = [];
  rows.forEach((r) => Object.keys(r).forEach((k) => { if (!k.startsWith('_') && !cols.includes(k)) cols.push(k); }));
  const identity = hasId && cols.includes('id');
  for (let i = 0; i < rows.length; i += 150) {
    const chunk = rows.slice(i, i + 150);
    out.push(`INSERT INTO ${table} (${cols.map((c) => (c === 'position' ? '"position"' : c)).join(', ')})${identity ? ' OVERRIDING SYSTEM VALUE' : ''} VALUES`);
    out.push(chunk.map((r) => `  (${cols.map((c) => lit(r[c])).join(', ')})`).join(',\n') + ';');
  }
}
ORDER.forEach(([table, hasId]) => {
  if (table === 'app_user') {
    out.push("INSERT INTO app_user (id, username, password_hash, role, full_name, phone, management_unit_id, cross_unit_access, must_change_password, locked, failed_login_count, locked_until, created_at, security_version, keycloak_user_id) OVERRIDING SYSTEM VALUE SELECT 1, username, password_hash, role, full_name, phone, NULL, FALSE, must_change_password, FALSE, 0, NULL, created_at, security_version, keycloak_user_id FROM keep_admin;");
  }
  if (table === 'management_unit') {
    // head_user_id tham chiếu app_user, bù sau khi tạo người dùng
    const rows = T.management_unit;
    const heads = rows.map((r) => ({ id: r.id, head: r.head_user_id }));
    rows.forEach((r) => (r.head_user_id = null));
    emit(table, hasId);
    rows.forEach((r, i) => (r.head_user_id = heads[i].head));
    return;
  }
  emit(table, hasId);
  if (table === 'app_user') out.push('UPDATE app_user u SET keycloak_user_id = k.keycloak_user_id FROM keep_kc k WHERE k.username = u.username AND u.id <> 1;');
  if (table === 'app_user') T.management_unit.forEach((u) => out.push(`UPDATE management_unit SET head_user_id = ${u.head_user_id} WHERE id = ${u.id};`));
});
ORDER.filter((o) => o[1]).forEach(([table]) => {
  if ((T[table] || []).length || table === 'app_user')
    out.push(`SELECT setval(pg_get_serial_sequence('${table}', 'id'), (SELECT COALESCE(MAX(id), 1) FROM ${table}), true);`);
});
out.push(`SELECT setval('seq_contract_code', ${T.dormitory_contract.length}, true);`);
out.push(`SELECT setval('seq_invoice_code', ${T.invoice.length}, true);`);
out.push(`SELECT setval('seq_maintenance_code', ${T.maintenance_request.length}, true);`);
out.push('COMMIT;');

const outDir = process.argv[2] || '.';
fs.mkdirSync(outDir, { recursive: true });
fs.writeFileSync(path.join(outDir, 'full-data.sql'), out.join('\n') + '\n', 'utf8');
const summary = ORDER.map(([t]) => `${t}=${(T[t] || []).length}`).join(' ');
console.log(summary);
