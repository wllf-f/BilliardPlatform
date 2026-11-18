// pages/compare/compare.js
const { get } = require('../../utils/request.js')
const { downloadListImages } = require('../../utils/image.js')

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    total: 0,
    categoryId: '',
    minPrice: '',
    maxPrice: '',
    sort: '',
    sortIndex: 0,
    sortNames: ['默认排序', '价格升序', '价格降序', '省最多'],
    sortValues: ['', 'price-asc', 'price-desc', 'save-desc'],
    categoryList: [],
    categoryNames: ['全部分类'],
    categoryIndex: 0,
    loading: false,
    noMore: false,
    priceFilterVisible: false
  },

  onLoad: function () {
    this.loadCategory()
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

  loadCategory: function () {
    get('/api/category/list').then(res => {
      const cats = res || []
      const names = ['全部分类'].concat(cats.map(c => c.name))
      this.setData({ categoryList: cats, categoryNames: names })
    }).catch(() => {})
  },

  onCategoryChange: function (e) {
    const idx = e.detail.value
    const cat = idx === 0 ? '' : (this.data.categoryList[idx - 1] ? this.data.categoryList[idx - 1].id : '')
    this.setData({ categoryIndex: idx, categoryId: cat })
    this.loadList(true)
  },

  onSortChange: function (e) {
    const idx = e.detail.value
    this.setData({
      sortIndex: idx,
      sort: this.data.sortValues[idx]
    })
    this.loadList(true)
  },

  showPriceFilter: function () {
    this.setData({ priceFilterVisible: true })
  },

  hidePriceFilter: function () {
    this.setData({ priceFilterVisible: false })
  },

  stopTap: function () {},

  onMinPrice: function (e) {
    this.setData({ minPrice: e.detail.value })
  },

  onMaxPrice: function (e) {
    this.setData({ maxPrice: e.detail.value })
  },

  resetPrice: function () {
    this.setData({ minPrice: '', maxPrice: '' })
  },

  confirmPrice: function () {
    this.setData({ priceFilterVisible: false })
    this.loadList(true)
  },

  loadList: function (reset) {
    if (this.data.loading) return Promise.resolve()
    const page = reset ? 1 : this.data.page + 1
    this.setData({ loading: true, page: page })
    if (reset) {
      this.setData({ noMore: false, list: [] })
    }
    const params = {
      page: page,
      size: this.data.size,
      categoryId: this.data.categoryId,
      minPrice: this.data.minPrice,
      maxPrice: this.data.maxPrice,
      sort: this.data.sort
    }
    var total = 0
    return get('/api/product/compare', params).then(res => {
      total = res.total || 0
      let records = res.records || []
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

  goDetail: function (e) {
    wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + e.currentTarget.dataset.id })
  }
})
