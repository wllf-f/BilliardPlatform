// utils/request.js 中央请求工具
const BASE_URL = 'http://127.0.0.1:8080'
console.log('[DEBUG request.js] 全局 BASE_URL =', BASE_URL)

function request(url, method, data) {
  const fullUrl = BASE_URL + url
  console.log('[DEBUG request] ' + (method || 'GET') + ' ' + fullUrl)
  if (data) console.log('[DEBUG request] 请求参数:', data)
  return new Promise((resolve, reject) => {
    wx.request({
      url: fullUrl,
      method: method || 'GET',
      data: data,
      header: {
        'token': wx.getStorageSync('token') || '',
        'Content-Type': 'application/json'
      },
      success: res => {
        console.log('[DEBUG request] 返回 HTTP ' + res.statusCode + ', data.code=' + (res.data && res.data.code != null ? res.data.code : 'N/A'))
        if (res.data && res.data.code === 200) {
          resolve(res.data.data)
        } else if (res.data && res.data.code === 401) {
          // token 失效，清除缓存并跳转登录
          wx.removeStorageSync('token')
          wx.removeStorageSync('userId')
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => {
            wx.redirectTo({ url: '/pages/login/login' })
          }, 800)
          reject(res.data)
        } else {
          console.error('[DEBUG request] 接口失败: msg=' + (res.data && res.data.msg))
          wx.showToast({ title: (res.data && res.data.msg) || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail: err => {
        console.error('[DEBUG request] 网络异常(可能连不上后端):', err)
        console.error('[DEBUG request] 请检查:')
        console.error('  1. 手机和电脑是否同一WiFi(192.168.31.x)?')
        console.error('  2. 防火墙是否放行8080端口?')
        console.error('  3. 电脑IP是否仍是' + BASE_URL + '?')
        wx.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

// 检查登录状态，未登录则跳转登录页
function checkLogin() {
  const token = wx.getStorageSync('token')
  if (!token) {
    wx.showModal({
      title: '提示',
      content: '请先登录后再操作',
      showCancel: false,
      success: function (res) {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/login/login' })
        }
      }
    })
    return false
  }
  return true
}

module.exports = {
  request,
  BASE_URL,
  checkLogin,
  get: (u, d) => request(u, 'GET', d),
  post: (u, d) => request(u, 'POST', d)
}
