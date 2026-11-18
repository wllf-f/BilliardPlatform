// pages/index/index.js
const { get } = require('../../utils/request.js')
const { downloadListImages } = require('../../utils/image.js')

Page({
  data: {
    hotList: [],
    categoryList: [],
    loading: true
  },

  onShow: function () {
    this.loadData()
  },

  onPullDownRefresh: function () {
    this.loadData().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  loadData: function () {
    return Promise.all([
      this.loadHot(),
      this.loadCategory()
    ]).catch(() => {})
  },

  loadHot: function () {
    return get('/api/product/hot', { limit: 8 }).then(res => {
      // 下载图片到本地临时路径
      return downloadListImages(res || [], 'mainImg')
    }).then(list => {
      this.setData({ hotList: list })
    }).catch(err => {
      console.error('[DEBUG 首页加载失败]:', err)
    })
  },

  loadCategory: function () {
    return get('/api/category/list').then(res => {
      this.setData({ categoryList: res || [], loading: false })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  goSearch: function () {
    wx.navigateTo({ url: '/pages/product-list/product-list?keyword=' })
  },

  goCategory: function () {
    wx.switchTab({ url: '/pages/category/category' })
  },

  goCompare: function () {
    wx.switchTab({ url: '/pages/compare/compare' })
  },

  goRecommend: function () {
    wx.navigateTo({ url: '/pages/recommend/recommend' })
  },

  goCustom: function () {
    wx.navigateTo({ url: '/pages/custom/custom' })
  },

  goTipModify: function () {
    wx.navigateTo({ url: '/pages/tip-modify/tip-modify' })
  },

  goPost: function () {
    wx.switchTab({ url: '/pages/post-list/post-list' })
  },

  goMoreHot: function () {
    wx.navigateTo({ url: '/pages/product-list/product-list' })
  },

  goDetail: function (e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + id })
  },

  goProductList: function (e) {
    const id = e.currentTarget.dataset.id
    const name = e.currentTarget.dataset.name
    wx.navigateTo({ url: '/pages/product-list/product-list?categoryId=' + id + '&categoryName=' + name })
  }
})
