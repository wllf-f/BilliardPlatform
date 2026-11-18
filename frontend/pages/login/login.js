// pages/login/login.js
const { post } = require('../../utils/request.js')

Page({
  data: {
    mode: 'login',
    username: '',
    password: ''
  },

  onInput: function (e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  switchTab: function (e) {
    this.setData({ mode: e.currentTarget.dataset.mode })
  },

  submit: function () {
    const { mode, username, password } = this.data
    if (!username || !password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
      return
    }
    const url = mode === 'login' ? '/api/user/login' : '/api/user/register'
    wx.showLoading({ title: '处理中...' })
    post(url, { username, password }).then(res => {
      wx.hideLoading()
      this.saveLoginInfo(res)
      wx.showToast({ title: mode === 'login' ? '登录成功' : '注册成功', icon: 'success' })
      setTimeout(() => {
        wx.switchTab({ url: '/pages/index/index' })
      }, 800)
    }).catch(() => {
      wx.hideLoading()
    })
  },

  wxLogin: function () {
    const that = this
    wx.getUserProfile({
      desc: '用于完善用户资料',
      success: function (info) {
        const userInfo = info.userInfo
        // 模拟 openid
        const openid = 'wx_' + Date.now()
        wx.showLoading({ title: '登录中...' })
        post('/api/user/wxLogin', {
          openid: openid,
          nickname: userInfo.nickName,
          avatar: userInfo.avatarUrl
        }).then(res => {
          wx.hideLoading()
          that.saveLoginInfo(res)
          wx.showToast({ title: '登录成功', icon: 'success' })
          setTimeout(() => {
            wx.switchTab({ url: '/pages/index/index' })
          }, 800)
        }).catch(() => {
          wx.hideLoading()
        })
      },
      fail: function () {
        wx.showToast({ title: '已取消授权', icon: 'none' })
      }
    })
  },

  saveLoginInfo: function (res) {
    wx.setStorageSync('token', res.token)
    wx.setStorageSync('userId', res.userId)
    wx.setStorageSync('nickname', res.nickname || '')
    wx.setStorageSync('avatar', res.avatar || '')
    const app = getApp()
    app.globalData.token = res.token
    app.globalData.userId = res.userId
  }
})
