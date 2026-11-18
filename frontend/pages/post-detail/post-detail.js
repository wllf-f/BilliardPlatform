// pages/post-detail/post-detail.js
const { get, post, checkLogin } = require('../../utils/request.js')
const { downloadImages, encodeImageUrl, preparePreviewUrls } = require('../../utils/image.js')

Page({
  data: {
    id: '',
    post: null,
    views: 0,
    likeCount: 0,
    liked: false,
    commentList: [],
    commentText: '',
    loading: true,
    isOwner: false
  },

  onLoad: function (options) {
    this.setData({ id: options.id })
    this.loadDetail()
    this.loadComments()
  },

  loadDetail: function () {
    var self = this
    get('/api/post/detail/' + this.data.id).then(res => {
      const post = res.post || {}
      if (post.images) {
        post.imgList = post.images.split(',').filter(Boolean)
      } else {
        post.imgList = []
      }
      // 1. 先用 URL 编码兜底
      post.imgList = post.imgList.map(function(u) { return encodeImageUrl(u) || u })
      if (post.avatar) post.avatar = encodeImageUrl(post.avatar) || post.avatar
      
      // 判断是否是帖子作者
      var currentUserId = wx.getStorageSync('userId')
      var isOwner = currentUserId && post.userId && String(currentUserId) === String(post.userId)
      
      // 2. 立即显示（不用等下载完）
      self.setData({
        post: post,
        views: res.views || 0,
        likeCount: res.likeCount || 0,
        liked: res.liked || false,
        isOwner: isOwner,
        loading: false
      })
      // 3. 异步下载到本地临时路径（移动端必须下载到本地才能显示远程图片）
      var imgUrls = post.imgList.slice()
      if (post.avatar) imgUrls.push(post.avatar)
      if (imgUrls.length > 0) {
        downloadImages(imgUrls).then(function(localUrls) {
          if (post.imgList.length > 0) {
            post.imgList = localUrls.slice(0, post.imgList.length)
          }
          if (post.avatar) {
            post.avatar = localUrls[localUrls.length - 1] || post.avatar
          }
          self.setData({ post: post })
        })
      }
    }).catch(() => {
      self.setData({ loading: false })
    })
  },

  loadComments: function () {
    var self = this
    get('/api/comment/list/' + this.data.id).then(res => {
      var comments = res || []
      // 先用 URL 编码兜底
      comments.forEach(function(c) {
        if (c.avatar) c.avatar = encodeImageUrl(c.avatar) || c.avatar
      })
      self.setData({ commentList: comments })
      // 异步下载评论头像到本地
      var avatarUrls = comments.map(function(c) { return c.avatar }).filter(Boolean)
      if (avatarUrls.length > 0) {
        downloadImages(avatarUrls).then(function(localUrls) {
          var curList = self.data.commentList.slice()
          var urlIndex = 0
          curList.forEach(function(c) {
            if (c.avatar && localUrls[urlIndex]) {
              c.avatar = localUrls[urlIndex++]
            }
          })
          self.setData({ commentList: curList })
        })
      }
    }).catch(() => {})
  },

  toggleLike: function () {
    if (!checkLogin()) return
    post('/api/post/like/' + this.data.id).then(res => {
      this.setData({
        liked: res.liked,
        likeCount: res.likeCount
      })
    }).catch(() => {})
  },

  onCommentInput: function (e) {
    this.setData({ commentText: e.detail.value })
  },

  sendComment: function () {
    if (!checkLogin()) return
    const text = this.data.commentText.trim()
    if (!text) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' })
      return
    }
    post('/api/comment/create', {
      postId: this.data.id,
      content: text
    }).then(res => {
      this.setData({ commentText: '' })
      wx.showToast({ title: '评论成功', icon: 'success' })
      this.loadComments()
    }).catch(() => {})
  },

  previewImg: function (e) {
    const currentUrl = e.currentTarget.dataset.url
    const urls = this.data.post && this.data.post.imgList ? this.data.post.imgList : []
    var self = this
    wx.previewImage({ current: currentUrl, urls: urls, fail: function() {
      wx.showLoading({ title: '加载预览中...' })
      preparePreviewUrls(urls).then(function(localUrls) {
        wx.hideLoading()
        var newIdx = urls.indexOf(currentUrl)
        var current = (newIdx >= 0 && localUrls[newIdx]) ? localUrls[newIdx] : localUrls[0]
        if (self.data.post) {
          self.data.post.imgList = localUrls
          self.setData({ post: self.data.post })
        }
        wx.previewImage({ current: current, urls: localUrls })
      })
    }})
  },

  toggleHidden: function () {
    var self = this
    wx.showModal({
      title: '提示',
      content: this.data.post.status === 3 ? '确定要公开这篇帖子吗？' : '确定要隐藏这篇帖子吗？隐藏后仅自己可见。',
      success: function (res) {
        if (res.confirm) {
          wx.showLoading({ title: '操作中...' })
          post('/api/post/toggle-hidden/' + self.data.id).then(function(res) {
            wx.hideLoading()
            wx.showToast({ title: '操作成功', icon: 'success' })
            // 重新加载详情
            self.loadDetail()
          }).catch(function(err) {
            wx.hideLoading()
            wx.showToast({ title: err.msg || '操作失败', icon: 'none' })
          })
        }
      }
    })
  },

  deletePost: function () {
    var self = this
    wx.showModal({
      title: '提示',
      content: '确定要删除这篇帖子吗？删除后不可恢复。',
      success: function (res) {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' })
          post('/api/post/delete/' + self.data.id).then(function(res) {
            wx.hideLoading()
            wx.showToast({ title: '删除成功', icon: 'success' })
            setTimeout(function() {
              wx.navigateBack()
            }, 1000)
          }).catch(function(err) {
            wx.hideLoading()
            wx.showToast({ title: err.msg || '删除失败', icon: 'none' })
          })
        }
      }
    })
  }
})
