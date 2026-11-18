// pages/product-list/product-list.js
const { get } = require('../../utils/request.js')
const { downloadListImages } = require('../../utils/image.js')

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    total: 0,
    keyword: '',
    categoryId: '',
    brandId: '',
    cueType: '',
    hardness: '',
    minPrice: '',
    maxPrice: '',
    sort: '',
    loading: false,
    noMore: false,
    filterVisible: false,
    selectMode: false       // 选择模式（从皮头改装进入）
  },

  onLoad: function (options) {
    if (options.categoryId) this.setData({ categoryId: options.categoryId })
    if (options.categoryName) wx.setNavigationBarTitle({ title: options.categoryName })
    if (options.brandId) this.setData({ brandId: options.brandId })
    if (options.keyword) this.setData({ keyword: options.keyword })
    if (options.cueType) this.setData({ cueType: options.cueType })
    if (options.selectMode) {
      this.setData({ selectMode: true })
      wx.setNavigationBarTitle({ title: '选择皮头商品' })
    }
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
    const params = {
      page: page,
      size: this.data.size,
      categoryId: this.data.categoryId,
      brandId: this.data.brandId,
      keyword: this.data.keyword,
      cueType: this.data.cueType,
      hardness: this.data.hardness,
      minPrice: this.data.minPrice,
      maxPrice: this.data.maxPrice,
      sort: this.data.sort
    }
    var total = 0
    return get('/api/product/list', params).then(res => {
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

  onSearchInput: function (e) {
    this.setData({ keyword: e.detail.value })
  },

  doSearch: function () {
    this.loadList(true)
  },

  changeSort: function (e) {
    this.setData({ sort: e.currentTarget.dataset.sort })
    this.loadList(true)
  },

  showFilter: function () {
    this.setData({ filterVisible: true })
  },

  hideFilter: function () {
    this.setData({ filterVisible: false })
  },

  stopTap: function () {},

  setCueType: function (e) {
    this.setData({ cueType: e.currentTarget.dataset.val })
  },

  setHardness: function (e) {
    this.setData({ hardness: e.currentTarget.dataset.val })
  },

  onMinPrice: function (e) {
    this.setData({ minPrice: e.detail.value })
  },

  onMaxPrice: function (e) {
    this.setData({ maxPrice: e.detail.value })
  },

  resetFilter: function () {
    this.setData({
      cueType: '',
      hardness: '',
      minPrice: '',
      maxPrice: ''
    })
  },

  confirmFilter: function () {
    this.setData({ filterVisible: false })
    this.loadList(true)
  },

  goDetail: function (e) {
    const id = e.currentTarget.dataset.id
    if (this.data.selectMode) {
      // 选择模式：跳转到商品详情（隐藏购买按钮）
      wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + id + '&hideBuy=true' })
      return
    }
    wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + id })
  },

  // 选择模式：确认选中商品并返回
  selectProduct: function (e) {
    const id = e.currentTarget.dataset.id
    const product = this.data.list.find(p => p.id === id)
    if (product) {
      const eventChannel = this.getOpenerEventChannel()
      eventChannel.emit('selectProduct', product)
      wx.navigateBack()
    }
  }
})
