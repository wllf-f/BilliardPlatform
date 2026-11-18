// pages/order/order.js
const { get, post, checkLogin } = require('../../utils/request.js')
const { downloadImages } = require('../../utils/image.js')

Page({
  data: {
    productId: '',
    product: null,
    address: null,
    quantity: 1,
    remark: '',
    totalAmount: 0,
    loading: true
  },

  onLoad: function (options) {
    this.setData({ productId: options.productId })
    this.loadProduct()
    this.loadDefaultAddress()
  },

  onShow: function () {
    // 从地址页返回时获取选中的地址
    const addr = wx.getStorageSync('selectedAddress')
    if (addr) {
      this.setData({ address: addr })
      wx.removeStorageSync('selectedAddress')
    }
  },

  loadDefaultAddress: function () {
    if (!checkLogin()) return
    get('/api/address/default').then(res => {
      if (res) {
        this.setData({ address: res })
      }
    }).catch(() => {})
  },

  loadProduct: function () {
    get('/api/product/detail/' + this.data.productId).then(res => {
      // 下载商品主图到本地临时路径
      return downloadImages([res.mainImg]).then(function(localUrls) {
        res.mainImg = localUrls[0] || res.mainImg
        return res
      })
    }).then(res => {
      this.setData({ product: res, loading: false })
      this.calcTotal()
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  selectAddress: function () {
    wx.navigateTo({ url: '/pages/address/address?selectMode=1' })
  },

  minusQty: function () {
    if (this.data.quantity > 1) {
      this.setData({ quantity: this.data.quantity - 1 })
      this.calcTotal()
    }
  },

  plusQty: function () {
    this.setData({ quantity: this.data.quantity + 1 })
    this.calcTotal()
  },

  onRemarkInput: function (e) {
    this.setData({ remark: e.detail.value })
  },

  calcTotal: function () {
    const price = this.data.product ? parseFloat(this.data.product.platformPrice) || 0 : 0
    this.setData({ totalAmount: (price * this.data.quantity).toFixed(2) })
  },

  submitOrder: function () {
    if (!checkLogin()) return
    if (!this.data.address) {
      wx.showToast({ title: '请选择收货地址', icon: 'none' })
      return
    }
    wx.showLoading({ title: '提交中...' })
    post('/api/order/create', {
      productId: this.data.productId,
      quantity: this.data.quantity,
      addressId: this.data.address.id,
      remark: this.data.remark
    }).then(res => {
      wx.hideLoading()
      wx.showToast({ title: '下单成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/order-list/order-list' })
      }, 1000)
    }).catch(() => {
      wx.hideLoading()
    })
  }
})
