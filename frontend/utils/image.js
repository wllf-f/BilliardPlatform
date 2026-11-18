// utils/image.js - 图片加载工具
// 解决微信小程序加载含中文路径的图片URL问题
// 采用并发控制 + 内存缓存 + 本地临时路径策略

// ★★★ 单一路由源：request.js 的 BASE_URL ★★★
// 如果服务器 IP 变更，只需修改 utils/request.js 的 BASE_URL，这里自动跟随
const { BASE_URL } = require('./request.js')
const SERVER_HOST = BASE_URL || ''
var imageCache = {}
// LRU：维护访问顺序，超过上限淘汰最早的，防止内存无限增长
var cacheKeys = []
var MAX_CACHE = 200
// 正在进行中的下载Promise：用于合并同一URL的并发请求
var downloading = {}

// 并发控制：最多同时下载3张
var CONCURRENCY = 3

/**
 * 对图片URL进行编码处理
 * 同时自动修复硬编码的 127.0.0.1:8080 为实际的服务器地址
 * 并能补全相对路径为完整URL
 * @param {string} url - 原始图片URL
 * @returns {string} 编码后的URL
 */
function encodeImageUrl(url) {
  if (!url || typeof url !== 'string') return url
  // 如果已经是本地临时路径，不编码
  if (url.indexOf('wxfile://') === 0 || url.indexOf('http://tmp') === 0) return url
  // ★★★ 处理相对路径（比如 billiard_img/avatars/1.jpg），补全为完整 URL ★★★
  if (url.indexOf('http://') === -1 && url.indexOf('https://') === -1) {
    if (url.charAt(0) === '/') {
      url = SERVER_HOST + url
    } else {
      url = SERVER_HOST + '/' + url
    }
  }
  // ★★★ 修复：替换硬编码的 127.0.0.1 为实际服务器地址 ★★★
  if (url.indexOf('127.0.0.1:8080') !== -1) {
    url = url.replace('127.0.0.1:8080', SERVER_HOST.replace('http://', ''))
  }
  // 已经编码过的也不再重复编码
  if (url.indexOf('%') !== -1 && /[\u4e00-\u9fa5]/.test(url) === false) {
    // 含百分号但无中文，可能已经编码
  }
  var parts = url.split('/')
  if (parts.length >= 4) {
    var pathParts = parts.slice(3)
    var needsEncode = false
    var encodedPath = pathParts.map(function(segment) {
      if (/[\u4e00-\u9fa5]/.test(segment) || /[（）]/.test(segment)) {
        needsEncode = true
        return encodeURIComponent(segment)
      }
      return segment
    }).join('/')
    if (!needsEncode) return url
    return parts[0] + '//' + parts[2] + '/' + encodedPath
  }
  return url
}

/**
 * 下载单个图片到本地临时路径（带缓存 + 重试）
 * @param {string} url - 原始图片URL
 * @returns {Promise<string>} 本地临时路径或编码后的URL（兜底）
 */
function downloadImage(url) {
  if (!url) return Promise.resolve(url)
  // 已经是本地路径直接返回
  if (url.indexOf('wxfile://') === 0 || url.indexOf('http://tmp') === 0) {
    return Promise.resolve(url)
  }
  // 外部可靠头像服务（dicebear、gravatar 等），直接显示，不下载
  if (url.indexOf('dicebear.com') !== -1 || url.indexOf('gravatar.com') !== -1 || url.indexOf('thirdwx.qlogo.cn') !== -1) {
    return Promise.resolve(encodeImageUrl(url))
  }
  // 命中缓存（LRU：移到末尾表示最近使用）
  if (imageCache[url]) {
    var idx = cacheKeys.indexOf(url)
    if (idx > -1) {
      cacheKeys.splice(idx, 1)
      cacheKeys.push(url)
    }
    return Promise.resolve(imageCache[url])
  }
  // 正在下载中，复用Promise
  if (downloading[url]) {
    return downloading[url]
  }
  var promise = new Promise(function(resolve) {
    var encodedUrl = encodeImageUrl(url)
    // 下载函数（带重试）
    function doDownload(retry) {
      wx.downloadFile({
        url: encodedUrl,
        timeout: 30000,
        success: function(res) {
          if (res.statusCode === 200 && res.tempFilePath) {
            imageCache[url] = res.tempFilePath
            cacheKeys.push(url)
            while (cacheKeys.length > MAX_CACHE) {
              var oldKey = cacheKeys.shift()
              delete imageCache[oldKey]
            }
            resolve(res.tempFilePath)
          } else if (retry > 0) {
            // 状态码非200，重试一次
            setTimeout(function() { doDownload(retry - 1) }, 500)
          } else {
            resolve(encodedUrl)
          }
        },
        fail: function() {
          if (retry > 0) {
            // 下载失败，重试一次
            setTimeout(function() { doDownload(retry - 1) }, 800)
          } else {
            resolve(encodedUrl)
          }
        }
      })
    }
    doDownload(1) // 尝试下载，失败则重试1次
  })
  downloading[url] = promise
  promise.then(function() { delete downloading[url] })
  return promise
}

/**
 * 并发控制下载
 * @param {Array<string>} urls - URL数组
 * @param {number} concurrency - 最大并发数
 * @returns {Promise<Array<string>>} 本地路径数组
 */
function downloadImages(urls, concurrency) {
  if (!urls || !Array.isArray(urls) || urls.length === 0) {
    return Promise.resolve(urls || [])
  }
  concurrency = concurrency || CONCURRENCY
  var results = new Array(urls.length)
  var index = 0

  function next() {
    if (index >= urls.length) return Promise.resolve()
    var currentIndex = index++
    return downloadImage(urls[currentIndex]).then(function(localPath) {
      results[currentIndex] = localPath
      return next()
    })
  }

  var workers = []
  for (var i = 0; i < Math.min(concurrency, urls.length); i++) {
    workers.push(next())
  }
  return Promise.all(workers).then(function() {
    return results
  })
}

/**
 * 下载商品列表中的图片（批量处理）
 * @param {Array<Object>} list - 商品列表
 * @param {string} field - 图片URL字段名
 * @returns {Promise<Array<Object>>} 处理后的商品列表
 */
function downloadListImages(list, field) {
  if (!list || !Array.isArray(list) || list.length === 0) {
    return Promise.resolve(list || [])
  }
  field = field || 'mainImg'
  var urls = list.map(function(item) { return item[field] }).filter(Boolean)
  return downloadImages(urls).then(function(localUrls) {
    var result = []
    var urlIndex = 0
    for (var i = 0; i < list.length; i++) {
      var item = Object.assign({}, list[i])
      if (item[field]) {
        item[field] = localUrls[urlIndex++] || item[field]
      }
      result.push(item)
    }
    return result
  })
}

/**
 * 清空图片缓存（通常在登出或缓存过大时调用）
 */
function clearImageCache() {
  imageCache = {}
  cacheKeys = []
  downloading = {}
}

/**
 * 批量将远程图片URL预下载到本地（用于 wx.previewImage 必须本地路径的场景）
 * @param {Array<string>} urls - 原始URL数组
 * @returns {Promise<Array<string>>} 本地路径数组（下载失败保留原编码URL）
 */
function preparePreviewUrls(urls) {
  if (!urls || !Array.isArray(urls)) return Promise.resolve([])
  var encoded = urls.map(function(u) { return encodeImageUrl(u) || u })
  return downloadImages(urls).then(function(localUrls) {
    return encoded.map(function(fallback, i) {
      var local = localUrls[i]
      if (local && (local.indexOf('wxfile://') === 0 || local.indexOf('http://tmp') === 0)) {
        return local
      }
      return fallback
    })
  })
}

module.exports = {
  encodeImageUrl: encodeImageUrl,
  downloadImage: downloadImage,
  downloadImages: downloadImages,
  downloadListImages: downloadListImages,
  clearImageCache: clearImageCache,
  preparePreviewUrls: preparePreviewUrls
}
