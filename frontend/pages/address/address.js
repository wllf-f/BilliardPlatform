// pages/address/address.js
const { get, post, checkLogin } = require('../../utils/request.js')
const { downloadImage, encodeImageUrl } = require('../../utils/image.js')

Page({
  data: {
    addressList: [],
    loading: true,
    // 模式控制
    selectMode: false,        // 选择地址返回给上一页
    orderConfirmMode: false,  // 订单确认模式（创建定制/改装订单）
    orderType: 0,             // 1普通 2定制 3改装
    from: '',
    // 订单确认相关
    selectedAddrId: '',
    quantity: 1,
    remark: '',
    customPrice: 5000,
    tipInfo: null,
    ownTotal: 70,
    totalPrice: 0,
    // 表单
    formVisible: false,
    editingAddress: null,
    regionValue: [],  // 省市区选择器的值：[省, 市, 区]
    addrForm: {
      consignee: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detail: '',
      isDefault: 0
    }
  },

  onLoad: function (options) {
    if (options.selectMode) {
      this.setData({ selectMode: true })
    }
    if (options.orderType && options.from) {
      this.setData({
        orderConfirmMode: true,
        orderType: parseInt(options.orderType),
        from: options.from
      })
      if (options.from === 'custom') {
        const form = wx.getStorageSync('customForm')
        if (form) this.setData({ customForm: form })
        const config = wx.getStorageSync('config')
        if (config && config.customCuePrice) this.setData({ customPrice: config.customCuePrice })
        this.setData({ totalPrice: this.data.customPrice })
      }
      if (options.from === 'tipModify') {
        const tipInfo = wx.getStorageSync('tipModifyInfo')
        if (tipInfo) {
          // 先对皮头商品图做URL编码兜底，并下载到本地
          if (tipInfo.product && tipInfo.product.mainImg) {
            tipInfo.product.mainImg = encodeImageUrl(tipInfo.product.mainImg) || tipInfo.product.mainImg
            downloadImage(tipInfo.product.mainImg).then(function(localPath) {
              tipInfo.product.mainImg = localPath
              wx.setStorageSync('tipModifyInfo', tipInfo)
            })
          }
          this.setData({ tipInfo: tipInfo })
          if (tipInfo.option === 'own') {
            this.setData({ totalPrice: this.data.ownTotal })
          } else if (tipInfo.product) {
            const price = parseFloat(tipInfo.product.platformPrice) || 0
            this.setData({ totalPrice: (price + 20).toFixed(2) })
          }
        }
      }
    }
    this.loadAddress()
  },

  onShow: function () {
    if (this.data.addressList.length > 0 || !this.data.loading) {
      // only reload if not initial load
    }
  },

  onPullDownRefresh: function () {
    this.loadAddress().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  loadAddress: function () {
    return get('/api/address/list').then(res => {
      this.setData({ addressList: res || [], loading: false })
      // 订单确认模式下自动选默认地址
      if (this.data.orderConfirmMode && !this.data.selectedAddrId && res && res.length > 0) {
        const def = res.find(a => a.isDefault === 1) || res[0]
        this.setData({ selectedAddrId: def.id })
      }
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  onSelectAddress: function (e) {
    const index = e.currentTarget.dataset.index
    const addr = this.data.addressList[index]
    if (this.data.selectMode) {
      // 选择地址返回给订单页
      wx.setStorageSync('selectedAddress', addr)
      wx.navigateBack()
    } else if (this.data.orderConfirmMode) {
      this.setData({ selectedAddrId: addr.id })
      this.calcTotal()
    }
  },

  calcTotal: function () {
    if (this.data.orderType === 2) {
      this.setData({ totalPrice: this.data.customPrice })
    } else if (this.data.orderType === 3 && this.data.tipInfo) {
      if (this.data.tipInfo.option === 'own') {
        this.setData({ totalPrice: this.data.ownTotal })
      } else if (this.data.tipInfo.product) {
        const price = parseFloat(this.data.tipInfo.product.platformPrice) || 0
        this.setData({ totalPrice: (price * this.data.quantity + 20).toFixed(2) })
      }
    }
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

  confirmOrder: function () {
    if (!checkLogin()) return
    if (!this.data.selectedAddrId) {
      wx.showToast({ title: '请选择收货地址', icon: 'none' })
      return
    }
    if (this.data.orderType === 2) {
      // 定制订单
      const form = wx.getStorageSync('customForm') || {}
      const data = Object.assign({}, form, {
        addressId: this.data.selectedAddrId,
        remark: this.data.remark
      })
      this.createOrder('/api/custom/create', data)
    } else if (this.data.orderType === 3) {
      // 皮头改装
      const tipInfo = this.data.tipInfo || {}
      if (tipInfo.option === 'own') {
        this.createOrder('/api/tipModify/own', {
          addressId: this.data.selectedAddrId,
          remark: this.data.remark
        })
      } else {
        this.createOrder('/api/tipModify/buy', {
          productId: tipInfo.productId,
          addressId: this.data.selectedAddrId,
          remark: this.data.remark
        })
      }
    }
  },

  createOrder: function (url, data) {
    wx.showLoading({ title: '提交中...' })
    post(url, data).then(res => {
      wx.hideLoading()
      // 清理缓存
      wx.removeStorageSync('customForm')
      wx.removeStorageSync('tipModifyInfo')
      wx.showToast({ title: '下单成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/order-list/order-list' })
      }, 1000)
    }).catch(() => {
      wx.hideLoading()
    })
  },

  addAddress: function () {
    this.setData({
      formVisible: true,
      editingAddress: null,
      regionValue: [],
      addrForm: {
        consignee: '',
        phone: '',
        province: '',
        city: '',
        district: '',
        detail: '',
        isDefault: 0
      }
    })
  },

  editAddress: function (e) {
    const index = e.currentTarget.dataset.index
    const addr = this.data.addressList[index]
    this.setData({
      formVisible: true,
      editingAddress: addr,
      regionValue: [addr.province || '', addr.city || '', addr.district || ''],
      addrForm: {
        id: addr.id,
        consignee: addr.consignee,
        phone: addr.phone,
        province: addr.province,
        city: addr.city,
        district: addr.district,
        detail: addr.detail,
        isDefault: addr.isDefault
      }
    })
  },

  /** 省市区选择器变更 */
  onRegionChange: function (e) {
    const val = e.detail.value  // [省, 市, 区]
    this.setData({
      regionValue: val,
      'addrForm.province': val[0] || '',
      'addrForm.city': val[1] || '',
      'addrForm.district': val[2] || ''
    })
  },

  onFormInput: function (e) {
    const field = e.currentTarget.dataset.field
    this.setData({ ['addrForm.' + field]: e.detail.value })
  },

  onDefaultChange: function (e) {
    this.setData({ 'addrForm.isDefault': e.detail.value ? 1 : 0 })
  },

  hideForm: function () {
    this.setData({ formVisible: false })
  },

  stopTap: function () {},

  saveAddress: function () {
    const f = this.data.addrForm
    if (!f.consignee) {
      wx.showToast({ title: '请输入收货人', icon: 'none' })
      return
    }
    if (!f.phone || f.phone.length !== 11) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    if (!f.province || !f.city || !f.detail) {
      wx.showToast({ title: '请完善地址信息', icon: 'none' })
      return
    }
    wx.showLoading({ title: '保存中...' })
    const url = this.data.editingAddress ? '/api/address/update' : '/api/address/add'
    post(url, f).then(res => {
      wx.hideLoading()
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.setData({ formVisible: false })
      this.loadAddress()
    }).catch(() => {
      wx.hideLoading()
    })
  },

  setDefault: function (e) {
    const id = e.currentTarget.dataset.id
    post('/api/address/setDefault/' + id).then(res => {
      wx.showToast({ title: '设置成功', icon: 'success' })
      this.loadAddress()
    }).catch(() => {})
  },

  deleteAddress: function (e) {
    const id = e.currentTarget.dataset.id
    const that = this
    wx.showModal({
      title: '提示',
      content: '确定删除该地址吗？',
      success: function (res) {
        if (res.confirm) {
          post('/api/address/delete/' + id).then(res => {
            wx.showToast({ title: '删除成功', icon: 'success' })
            that.loadAddress()
          }).catch(() => {})
        }
      }
    })
  }
})
