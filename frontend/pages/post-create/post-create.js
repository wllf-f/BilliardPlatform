// pages/post-create/post-create.js
const { post, checkLogin, BASE_URL } = require('../../utils/request.js')

Page({
  data: {
    title: '',
    content: '',
    images: []
  },

  onTitleInput: function (e) {
    this.setData({ title: e.detail.value })
  },

  onContentInput: function (e) {
    this.setData({ content: e.detail.value })
  },

  chooseImg: function () {
    const that = this
    wx.chooseImage({
      count: 9 - this.data.images.length,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: function (res) {
        const tempPaths = res.tempFilePaths
        that.setData({
          images: that.data.images.concat(tempPaths)
        })
      }
    })
  },

  delImg: function (e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.images
    images.splice(index, 1)
    this.setData({ images: images })
  },

  // 上传单个图片
  uploadOneImage: function (filePath) {
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: BASE_URL + '/api/upload/image',
        filePath: filePath,
        name: 'file',
        header: {
          'token': wx.getStorageSync('token') || ''
        },
        success: res => {
          const data = JSON.parse(res.data)
          if (data.code === 200) {
            resolve(data.data.url)
          } else {
            reject(data.msg || '上传失败')
          }
        },
        fail: err => {
          reject(err)
        }
      })
    })
  },

  submit: function () {
    if (!checkLogin()) return
    if (!this.data.title.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' })
      return
    }
    if (!this.data.content.trim()) {
      wx.showToast({ title: '请输入内容', icon: 'none' })
      return
    }

    wx.showLoading({ title: '发布中...' })

    // 先上传所有图片
    const uploadPromises = this.data.images.map(path => this.uploadOneImage(path))
    Promise.all(uploadPromises).then(serverUrls => {
      // 提交帖子
      return post('/api/post/create', {
        title: this.data.title,
        content: this.data.content,
        images: serverUrls.join(',')
      })
    }).then(res => {
      wx.hideLoading()
      wx.showToast({ title: '发布成功', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 1000)
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: err.msg || '发布失败', icon: 'none' })
    })
  }
})
