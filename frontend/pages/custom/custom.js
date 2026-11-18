// pages/custom/custom.js
const { get, post, checkLogin } = require('../../utils/request.js')

Page({
  data: {
    customPrice: 5000,
    config: {},
    form: {
      playType: '',
      powerStyle: '',
      techPref: '',
      feelChoice: '',
      feelDesc: '',
      oldCue: '',
      cueStructure: '',
      totalLength: '',
      totalWeight: '',
      balancePoint: '',
      frontJointModel: '',
      tipDiameter: '',
      gripDiameter: '',
      taperReq: '',
      tipMaterial: '',
      tipSpec: '',
      baseWood: '',
      gripType: '',
      insertPlan: '',
      inlayConfig: '',
      lacquerReq: '',
      decorRemark: '',
      grainClause: ''
    }
  },

  onLoad: function () {
    this.loadConfig()
  },

  loadConfig: function () {
    get('/api/config/common').then(res => {
      this.setData({ config: res || {} })
      if (res && res.customCuePrice) {
        this.setData({ customPrice: res.customCuePrice })
      }
    }).catch(() => {})
  },

  onInput: function (e) {
    const field = e.currentTarget.dataset.field
    this.setData({ ['form.' + field]: e.detail.value })
  },

  contactService: function () {
    const cs = this.data.config.customerService || '请联系客服咨询定制详情'
    wx.showModal({
      title: '联系客服',
      content: cs,
      showCancel: false
    })
  },

  confirmCustom: function () {
    if (!checkLogin()) return
    // 校验必填
    const f = this.data.form
    if (!f.playType) {
      wx.showToast({ title: '请填写打球类型', icon: 'none' })
      return
    }
    if (!f.cueStructure) {
      wx.showToast({ title: '请填写球杆结构', icon: 'none' })
      return
    }
    // 存储定制表单，跳转地址页
    wx.setStorageSync('customForm', this.data.form)
    wx.navigateTo({
      url: '/pages/address/address?orderType=2&from=custom'
    })
  }
})
