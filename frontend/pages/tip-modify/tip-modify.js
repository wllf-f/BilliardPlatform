// pages/tip-modify/tip-modify.js
const { get, checkLogin } = require('../../utils/request.js')
const { downloadImage, encodeImageUrl } = require('../../utils/image.js')

Page({
  data: {
    option: '',
    config: {},
    remark: '',
    selectedProduct: null,
    tipModifyFee: 50,
    tipModifyHandfee: 20,
    ownTotal: 70,
    totalPrice: 0
  },

  onLoad: function () {
    this.loadConfig()
  },

  loadConfig: function () {
    get('/api/config/common').then(res => {
      const config = res || {}
      const fee = parseFloat(config.tipModifyFee) || 50
      const handfee = parseFloat(config.tipModifyHandfee) || 20
      this.setData({
        config: config,
        tipModifyFee: fee,
        tipModifyHandfee: handfee,
        ownTotal: fee + handfee
      })
    }).catch(() => {})
  },

  selectOption: function (e) {
    const option = e.currentTarget.dataset.option
    this.setData({ option: option })
    this.calcTotal()
  },

  onRemarkInput: function (e) {
    this.setData({ remark: e.detail.value })
  },

  selectProduct: function () {
    const that = this
    wx.navigateTo({
      url: '/pages/product-list/product-list?keyword=皮头&selectMode=true',
      events: {
        selectProduct: function (data) {
          // 选择皮头后先URL编码兜底，再下载本地路径
          var product = data
          if (product && product.mainImg) {
            product.mainImg = encodeImageUrl(product.mainImg) || product.mainImg
          }
          that.setData({ selectedProduct: product })
          that.calcTotal()
          if (product && product.mainImg) {
            downloadImage(product.mainImg).then(function(localPath) {
              // 通过 setData 更新（注意引用关系）
              var p = that.data.selectedProduct
              if (p) {
                p.mainImg = localPath
                that.setData({ selectedProduct: p })
                // 同步回 storage
                var tipInfo = wx.getStorageSync('tipModifyInfo') || {}
                tipInfo.product = p
                wx.setStorageSync('tipModifyInfo', tipInfo)
              }
            })
          }
        }
      }
    })
  },

  calcTotal: function () {
    let total = 0
    if (this.data.option === 'own') {
      total = this.data.ownTotal
    } else if (this.data.option === 'buy' && this.data.selectedProduct) {
      total = (parseFloat(this.data.selectedProduct.platformPrice) || 0) + this.data.tipModifyHandfee
    }
    this.setData({ totalPrice: total })
  },

  confirmTipModify: function () {
    if (!checkLogin()) return
    if (!this.data.option) {
      wx.showToast({ title: '请选择改装方式', icon: 'none' })
      return
    }
    if (this.data.option === 'buy' && !this.data.selectedProduct) {
      wx.showToast({ title: '请选择皮头商品', icon: 'none' })
      return
    }
    // 存储改装信息，跳转地址页
    const tipInfo = {
      option: this.data.option,
      remark: this.data.remark,
      productId: this.data.selectedProduct ? this.data.selectedProduct.id : '',
      product: this.data.selectedProduct
    }
    wx.setStorageSync('tipModifyInfo', tipInfo)
    wx.navigateTo({
      url: '/pages/address/address?orderType=3&from=tipModify'
    })
  }
})
