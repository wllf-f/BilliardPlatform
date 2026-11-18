// pages/user-edit/user-edit.js
const { get, post, BASE_URL } = require('../../utils/request.js')
const { downloadImage, encodeImageUrl } = require('../../utils/image.js')

Page({
  data: {
    userInfo: {},
    nickname: '',
    phone: ''
  },

  onLoad: function () {
    this.loadUserInfo()
  },

  loadUserInfo: function () {
    var self = this
    get('/api/user/info').then(res => {
      var userInfo = res || {}
      self.setData({
        userInfo: userInfo,
        nickname: userInfo.nickname || '',
        phone: userInfo.phone || ''
      })
      // 下载头像到本地
      if (userInfo.avatar) {
        var encoded = encodeImageUrl(userInfo.avatar)
        downloadImage(encoded).then(function(localPath) {
          self.setData({ 'userInfo.avatar': localPath })
        })
      }
    }).catch(() => {})
  },

  onNicknameInput: function (e) {
    this.setData({ nickname: e.detail.value })
  },

  onPhoneInput: function (e) {
    this.setData({ phone: e.detail.value })
  },

  changeAvatar: function () {
    var self = this
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: function (res) {
        var tempPath = res.tempFilePaths[0]
        self.uploadAvatar(tempPath)
      }
    })
  },

  uploadAvatar: function (filePath) {
    var self = this
    wx.showLoading({ title: '上传中...' })
    wx.uploadFile({
      url: BASE_URL + '/api/user/avatar',
      filePath: filePath,
      name: 'file',
      header: { 'token': wx.getStorageSync('token') || '' },
      success: function (res) {
        wx.hideLoading()
        try {
          var data = JSON.parse(res.data)
          if (data.code === 200) {
            wx.showToast({ title: '头像更新成功', icon: 'success' })
            // 重新加载用户信息
            self.loadUserInfo()
          } else {
            wx.showToast({ title: data.msg || '上传失败', icon: 'none' })
          }
        } catch (e) {
          wx.showToast({ title: '上传失败', icon: 'none' })
        }
      },
      fail: function () {
        wx.hideLoading()
        wx.showToast({ title: '上传失败', icon: 'none' })
      }
    })
  },

  save: function () {
    var self = this
    var nickname = this.data.nickname.trim()
    var phone = this.data.phone.trim()

    if (!nickname) {
      wx.showToast({ title: '请输入昵称', icon: 'none' })
      return
    }

    wx.showLoading({ title: '保存中...' })
    // 先保存昵称
    post('/api/user/nickname', { nickname: nickname }).then(function() {
      // 如果填了手机号，再保存手机号
      if (phone) {
        return post('/api/user/phone', { phone: phone })
      }
    }).then(function() {
      wx.hideLoading()
      wx.showToast({ title: '保存成功', icon: 'success' })
      // 更新本地缓存的昵称
      wx.setStorageSync('nickname', nickname)
      setTimeout(function() {
        wx.navigateBack()
      }, 1000)
    }).catch(function(err) {
      wx.hideLoading()
      wx.showToast({ title: (err && err.msg) || '保存失败', icon: 'none' })
    })
  }
})
