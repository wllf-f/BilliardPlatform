// pages/profile/profile.js
const { get, post, BASE_URL } = require('../../utils/request.js')
const { downloadImage } = require('../../utils/image.js')

Page({
  data: {
    isLogin: false,
    userInfo: {},
    config: {}
  },

  onShow: function () {
    const token = wx.getStorageSync('token')
    if (token) {
      this.setData({ isLogin: true })
      this.loadUserInfo()
    } else {
      this.setData({ isLogin: false, userInfo: {} })
    }
    this.loadConfig()
  },

  loadUserInfo: function () {
    get('/api/user/info').then(res => {
      const userInfo = res || {}
      // 下载头像到本地临时路径
      if (userInfo.avatar) {
        downloadImage(userInfo.avatar).then(function(localPath) {
          userInfo.avatar = localPath
          this.setData({ userInfo: userInfo })
        }.bind(this))
      } else {
        this.setData({ userInfo: userInfo })
      }
    }).catch(() => {})
  },

  loadConfig: function () {
    get('/api/config/common').then(res => {
      this.setData({ config: res || {} })
      wx.setStorageSync('config', res || {})
    }).catch(() => {})
  },

  goLogin: function () {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  goEdit: function () {
    wx.navigateTo({ url: '/pages/user-edit/user-edit' })
  },

  goOrderList: function () {
    if (!this.data.isLogin) { this.goLogin(); return }
    wx.navigateTo({ url: '/pages/order-list/order-list' })
  },

  goOrderListByStatus: function (e) {
    if (!this.data.isLogin) { this.goLogin(); return }
    const status = e.currentTarget.dataset.status
    wx.navigateTo({ url: '/pages/order-list/order-list?status=' + status })
  },

  goCollection: function () {
    if (!this.data.isLogin) { this.goLogin(); return }
    wx.navigateTo({ url: '/pages/collection/collection' })
  },

  goMyPost: function () {
    if (!this.data.isLogin) { this.goLogin(); return }
    wx.navigateTo({ url: '/pages/my-post/my-post' })
  },

  goAddress: function () {
    if (!this.data.isLogin) { this.goLogin(); return }
    wx.navigateTo({ url: '/pages/address/address' })
  },

  goCustom: function () {
    wx.navigateTo({ url: '/pages/custom/custom' })
  },

  goTipModify: function () {
    wx.navigateTo({ url: '/pages/tip-modify/tip-modify' })
  },

  goRecommend: function () {
    wx.navigateTo({ url: '/pages/recommend/recommend' })
  },

  contactService: function () {
    const cs = this.data.config.customerService || '客服微信：biliiard-service'
    wx.showModal({
      title: '联系客服',
      content: cs,
      showCancel: false
    })
  },

  aboutUs: function () {
    const about = this.data.config.aboutUs || '台球器材优选平台，致力于为球友提供专业的台球器材选购服务。'
    wx.showModal({
      title: '关于我们',
      content: about,
      showCancel: false
    })
  },

  logout: function () {
    const that = this
    wx.showModal({
      title: '提示',
      content: '确定退出登录吗？',
      success: function (res) {
        if (res.confirm) {
          post('/api/user/logout').then(() => {
            that.clearLogin()
          }).catch(() => {
            that.clearLogin()
          })
        }
      }
    })
  },

  clearLogin: function () {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userId')
    wx.removeStorageSync('nickname')
    wx.removeStorageSync('avatar')
    this.setData({ isLogin: false, userInfo: {} })
    wx.showToast({ title: '已退出', icon: 'success' })
  }
})
