// pages/collection/collection.js
const { get, post } = require('../../utils/request.js')
const { downloadListImages } = require('../../utils/image.js')

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    total: 0,
    loading: false,
    noMore: false
  },

  onLoad: function () {
    this.loadList(true)
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
    return get('/api/collection/list', { page: page, size: this.data.size }).then(res => {
      total = res.total || 0
      var records = res.records || []
      // 下载图片到本地临时路径
      return downloadListImages(records, 'mainImg')
    }).then(records => {
      const newList = reset ? records : this.data.list.concat(records)
      this.setData({
        list: newList,
        total: total,
        loading: false,
        noMore: newList.length >= total
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  cancelCollect: function (e) {
    const productId = e.currentTarget.dataset.id
    const index = e.currentTarget.dataset.index
    const that = this
    wx.showModal({
      title: '提示',
      content: '确定取消收藏该商品吗？',
      success: function (res) {
        if (res.confirm) {
          post('/api/collection/toggle/' + productId).then(data => {
            wx.showToast({ title: '已取消收藏', icon: 'success' })
            const list = that.data.list
            list.splice(index, 1)
            that.setData({ list: list })
          }).catch(() => {})
        }
      }
    })
  },

  goDetail: function (e) {
    wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + e.currentTarget.dataset.id })
  }
})
