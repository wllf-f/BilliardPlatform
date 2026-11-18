// pages/post-list/post-list.js
const { get, post, checkLogin } = require('../../utils/request.js')
const { downloadImages, encodeImageUrl } = require('../../utils/image.js')

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    total: 0,
    keyword: '',
    loading: false,
    noMore: false
  },

  onLoad: function () {
    this.loadList(true)
  },

  onShow: function () {
    if (this.data.list.length > 0) {
      this.loadList(true)
    }
  },

  onPullDownRefresh: function () {
    this.loadList(true).then(() => {
      wx.stopPullDownRefresh()
    })
  },

  onReachBottom: function () {
    if (!this.data.noMore && !this.data.loading) {
      this.loadList(false)
    }
  },

  loadList: function (reset) {
    if (this.data.loading) return Promise.resolve()
    const page = reset ? 1 : this.data.page + 1
    this.setData({ loading: true, page: page })
    if (reset) {
      this.setData({ noMore: false, list: [] })
    }
    var total = 0
    var self = this
    // ★★★ 在 setData 之前就记录好原列表长度（用于加载更多时计算偏移） ★★★
    var origLen = self.data.list.length
    return get('/api/post/list', {
      page: page,
      size: this.data.size,
      keyword: this.data.keyword
    }).then(res => {
      total = res.total || 0
      var records = (res.records || []).map(function(item) {
        var newItem = Object.assign({}, item)
        if (newItem.images) {
          newItem.imgList = newItem.images.split(',').filter(Boolean)
        } else {
          newItem.imgList = []
        }
        // 先用 URL 编码兜底
        newItem.imgList = newItem.imgList.map(function(u) { return encodeImageUrl(u) || u })
        if (newItem.avatar) newItem.avatar = encodeImageUrl(newItem.avatar) || newItem.avatar
        return newItem
      })
      // 立即 setData：先用 URL 编码的图片填充，避免白屏
      var newList = reset ? records : self.data.list.concat(records)
      self.setData({
        list: newList,
        total: total,
        loading: false,
        noMore: newList.length >= total
      })
      // 如果用户已登录，异步查询点赞状态
      self.checkBatchLiked()
      // 异步下载所有图片到本地，成功后更新
      var allImgUrls = []
      var urlMap = [] // {listIdx, subIdx(-1=avatar), type}
      records.forEach(function(item, ri) {
        var listIdx = reset ? ri : (origLen + ri)
        if (item.imgList && item.imgList.length > 0) {
          item.imgList.forEach(function(u, ji) {
            if (u && u.indexOf('wxfile://') !== 0 && u.indexOf('http://tmp') !== 0) {
              allImgUrls.push(u)
              urlMap.push({ listIdx: listIdx, subIdx: ji, type: 'img' })
            }
          })
        }
        if (item.avatar && item.avatar.indexOf('wxfile://') !== 0 && item.avatar.indexOf('http://tmp') !== 0) {
          allImgUrls.push(item.avatar)
          urlMap.push({ listIdx: listIdx, subIdx: -1, type: 'avatar' })
        }
      })
      if (allImgUrls.length > 0) {
        downloadImages(allImgUrls).then(function(localUrls) {
          var curList = self.data.list.slice()
          localUrls.forEach(function(localPath, ui) {
            if (!localPath || !urlMap[ui]) return
            var info = urlMap[ui]
            var item = curList[info.listIdx]
            if (!item) return
            if (info.type === 'avatar') {
              item.avatar = localPath
            } else if (info.type === 'img' && item.imgList && item.imgList[info.subIdx] !== undefined) {
              item.imgList[info.subIdx] = localPath
            }
          })
          self.setData({ list: curList })
        })
      }
    }).catch(function() {
      self.setData({ loading: false })
    })
  },

  onSearchInput: function (e) {
    this.setData({ keyword: e.detail.value })
  },

  doSearch: function () {
    this.loadList(true)
  },

  goDetail: function (e) {
    wx.navigateTo({ url: '/pages/post-detail/post-detail?id=' + e.currentTarget.dataset.id })
  },

  toggleLike: function (e) {
    if (!checkLogin()) return
    var self = this
    var id = e.currentTarget.dataset.id
    var index = e.currentTarget.dataset.index
    post('/api/post/like/' + id).then(function(res) {
      var key = 'list[' + index + '].liked'
      var cntKey = 'list[' + index + '].likeCount'
      self.setData({
        [key]: res.liked,
        [cntKey]: res.likeCount
      })
    }).catch(function() {})
  },

  checkBatchLiked: function () {
    var self = this
    var ids = self.data.list.map(function(item) { return item.id }).filter(Boolean)
    if (ids.length === 0) return
    post('/api/post/liked/batch', { ids: ids }).then(function(likedMap) {
      if (!likedMap) return
      var data = {}
      self.data.list.forEach(function(item, i) {
        if (likedMap[item.id] !== undefined) {
          data['list[' + i + '].liked'] = !!likedMap[item.id]
        }
      })
      if (Object.keys(data).length > 0) {
        self.setData(data)
      }
    }).catch(function() {})
  },

  goCreate: function () {
    if (!checkLogin()) return
    wx.navigateTo({ url: '/pages/post-create/post-create' })
  }
})
