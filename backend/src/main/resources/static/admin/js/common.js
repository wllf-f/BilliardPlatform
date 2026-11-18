/* ===== 管理后台公共请求工具 ===== */
// ⚡ 如果 IP 变了，只需修改这一行！
const BASE_URL = 'http://127.0.0.1:8080';
// 商品图片资源根路径
const IMG_BASE = BASE_URL + '/billiard_img/';

/**
 * 通用 AJAX 请求封装
 * @param url    接口地址（以 / 开头）
 * @param method 请求方法 GET/POST
 * @param data   请求数据
 * @param callback 成功回调（参数为 res.data）
 */
function ajax(url, method, data, callback, errCallback) {
  $.ajax({
    url: BASE_URL + url,
    type: method || 'GET',
    data: method === 'GET' ? data : JSON.stringify(data),
    contentType: 'application/json',
    headers: { 'token': localStorage.getItem('adminToken') || '' },
    success: function (res) {
      if (res.code === 200) { callback && callback(res.data); }
      else if (res.code === 401) { location.href = '../login.html'; }
      else {
        if (errCallback) { errCallback(res); }
        else { alert(res.msg || '操作失败'); }
      }
    },
    error: function (xhr) {
      if (xhr.status === 401) { location.href = '../login.html'; }
      else if (errCallback) { errCallback(xhr); }
      else { alert('请求失败：' + (xhr.statusText || '网络错误')); }
    }
  });
}

/* 退出登录（在 index.html 中调用） */
function adminLogout() {
  $.ajax({
    url: BASE_URL + '/admin/logout',
    type: 'POST',
    contentType: 'application/json',
    headers: { 'token': localStorage.getItem('adminToken') || '' },
    complete: function () {
      localStorage.removeItem('adminToken');
      localStorage.removeItem('adminInfo');
      // 清除 Cookie 中的登录凭证，后端拦截器将重定向到登录页
      document.cookie = 'adminToken=; path=/admin; max-age=0';
      location.href = 'login.html';
    }
  });
}

/* ===== 通用辅助函数 ===== */

// 内联 SVG 占位图（无需网络请求，兜底用）
const PLACEHOLDER_SVG = 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="60" height="60" viewBox="0 0 60 60">' +
  '<rect fill="#f0f0f0" width="60" height="60"/>' +
  '<text x="30" y="30" text-anchor="middle" font-size="10" fill="#bbb" dominant-baseline="middle">无图</text>' +
  '</svg>'
);

// 通用占位图（当商品图片缺失时使用）
const DEFAULT_PRODUCT_IMG = 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="60" height="60" viewBox="0 0 60 60">' +
  '<rect fill="#FFF3E0" width="60" height="60"/>' +
  '<circle cx="30" cy="22" r="10" fill="#FFB74D"/>' +
  '<rect x="15" y="34" width="30" height="15" rx="3" fill="#FFB74D"/>' +
  '</svg>'
);

// 拼接商品主图地址（支持完整URL / 相对路径 / 空值兜底）
// 返回值已过 esc，可直接嵌到 img src 属性中，避免因路径含引号导致 stored XSS
function imgUrl(imgDir, mainImg) {
  // 兼容调用：imgUrl(path) 单参数模式
  if (typeof mainImg === 'undefined') {
    var path = imgDir || '';
    var full;
    if (!path) full = DEFAULT_PRODUCT_IMG;
    else if (path.indexOf('http') === 0) full = path;
    else if (path.indexOf('data:') === 0) full = path;
    else if (path.charAt(0) === '/') full = BASE_URL + path;
    // 已带 billiard_img/ 前缀的相对路径，直接补域名，避免重复前缀导致 404
    else if (path.indexOf('billiard_img/') === 0) full = BASE_URL + '/' + path;
    else full = IMG_BASE + path;
    return esc(full);
  }
  // 双参数模式：imgUrl(imgDir, mainImg)
  var full2;
  if (!mainImg) full2 = DEFAULT_PRODUCT_IMG;
  else {
    var path2 = (imgDir || '') + mainImg;
    if (path2.indexOf('http') === 0) full2 = path2;
    else full2 = IMG_BASE + path2;
  }
  return esc(full2);
}

// 商品行图：从后端返回的 imgUrl 字符串直接处理（返回值已 HTML 属性转义）
function productImg(src) {
  var full;
  if (!src) full = DEFAULT_PRODUCT_IMG;
  else if (src.indexOf('http') === 0 || src.indexOf('data:') === 0) full = src;
  else if (src.charAt(0) === '/') full = BASE_URL + src;
  else full = IMG_BASE + src;
  return esc(full);
}

// 拼接用户头像地址（相对路径补全为绝对路径）
function avatarUrl(path) {
  if (!path) return '';
  if (path.indexOf('http') === 0) return path;
  return BASE_URL + '/' + path;
}

// 转义 HTML，防止 XSS
function esc(str) {
  if (str === null || str === undefined) return '';
  return String(str).replace(/[&<>"']/g, function (m) {
    return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m];
  });
}

// 简单分页控件渲染
function renderPager(container, page, size, total, go) {
  var pages = Math.max(1, Math.ceil(total / size));
  var html = '<nav><ul class="pagination pagination-sm">';
  html += '<li class="page-item ' + (page <= 1 ? 'disabled' : '') + '"><a class="page-link" href="javascript:;">上一页</a></li>';
  var start = Math.max(1, page - 2), end = Math.min(pages, page + 2);
  if (start > 1) { html += '<li class="page-item"><a class="page-link" href="javascript:;">1</a></li>'; if (start > 2) html += '<li class="page-item disabled"><span class="page-link">...</span></li>'; }
  for (var i = start; i <= end; i++) {
    html += '<li class="page-item ' + (i === page ? 'active' : '') + '"><a class="page-link" href="javascript:;">' + i + '</a></li>';
  }
  if (end < pages) { if (end < pages - 1) html += '<li class="page-item disabled"><span class="page-link">...</span></li>'; html += '<li class="page-item"><a class="page-link" href="javascript:;">' + pages + '</a></li>'; }
  html += '<li class="page-item ' + (page >= pages ? 'disabled' : '') + '"><a class="page-link" href="javascript:;">下一页</a></li>';
  html += '</ul></nav>';
  var info = '共 ' + total + ' 条，第 ' + page + '/' + pages + ' 页';
  $(container).html('<div class="info">' + info + '</div>' + html);
  $(container).find('.page-item:not(.disabled) .page-link').each(function () {
    var txt = $(this).text();
    var p;
    if (txt === '上一页') p = page - 1;
    else if (txt === '下一页') p = page + 1;
    else p = parseInt(txt);
    if (!isNaN(p)) {
      var target = p;
      $(this).on('click', function () { go(target); });
    }
  });
}

// 格式化时间为 yyyy-MM-dd HH:mm:ss
function fmtTime(t) {
  if (!t) return '-';
  // 移除 ISO 8601 的 T 分隔符
  var s = (typeof t === 'number') ? t : String(t).replace('T', ' ').replace(/-/g, '/');
  var d = new Date(s);
  if (isNaN(d)) return t;
  var p = function (n) { return n < 10 ? '0' + n : n; };
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
}

// 金额格式化（保留 2 位）
function fmtMoney(n) {
  if (n === null || n === undefined || n === '') return '0.00';
  return Number(n).toFixed(2);
}
