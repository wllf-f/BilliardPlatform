// pages/order-detail/order-detail.js
const { get, post } = require('../../utils/request.js')
const { downloadImages } = require('../../utils/image.js')

const STATUS_MAP = {
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消'
}

const TYPE_MAP = {
  1: '普通商品',
  2: '仁杆定制',
  3: '更换'
}

// 格式化时间：去掉中间的 T
function fmtTime(t) {
  if (!t || t === '-') return '-'
  return String(t).replace('T', ' ')
}

Page({
  data: {
    orderDetail: {},
    statusText: '',
    orderTypeText: '',
    countdown: '',
    receiveCountdown: '',
    countdownTimer: null,
    tipModifyAddress: ''
  },

  onLoad: function (options) {
    // 先加载配置
    get('/api/config/common').then(config => {
      if (config && config.tipModifyAddress) {
        this.setData({ tipModifyAddress: config.tipModifyAddress })
      }
    }).catch(() => {})
    if (options.id) {
      this.loadDetail(options.id)
    }
  },

  onUnload: function () {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer)
    }
  },

  loadDetail: function (id) {
    wx.showLoading({ title: '加载中...' })
    get('/api/order/detail/' + id).then(data => {
      data.statusText = STATUS_MAP[data.status] || '未知'
      data.orderTypeText = TYPE_MAP[data.orderType] || '普通商品'
      // 用 createTimeDisplay/shipTimeDisplay 等展示（去掉 T），保留原始时间用于倒计时计算
      data.createTimeDisplay = fmtTime(data.createTime)
      data.payTimeDisplay = fmtTime(data.payTime)
      data.shipTimeDisplay = fmtTime(data.shipTime)
      data.finishTimeDisplay = fmtTime(data.finishTime)
      // 给定制/更换订单添加默认商品图片
      if (data.orderType === 2 || data.orderType === 3) {
        if (!data.items || data.items.length === 0) {
          data.items = [{
            productName: data.orderType === 2 ? '仁杆定制' : '皮头更换',
            price: data.totalAmount,
            quantity: 1,
            subtotal: data.totalAmount,
            imgUrl: ''
          }]
        }
        // 服务类订单：优先使用本地 PNG 图（后端 SVG 在微信小程序中兼容性差）
        const localImg = data.orderType === 2 ? '/images/仁杆图片.png' : '/images/皮头更换.png'
        data.items.forEach(g => {
          // 如果后端给的是 SVG 或空值，替换为本地图
          if (!g.imgUrl || g.imgUrl.indexOf('.svg') > -1 || g.imgUrl.indexOf('service/') > -1) {
            g.imgUrl = localImg
          }
        })
      }
      // 下载商品图片到本地临时路径
      var imgUrls = []
      if (data.items) {
        data.items.forEach(function(g) {
          if (g.imgUrl && g.imgUrl !== '/images/仁杆图片.png' && g.imgUrl !== '/images/皮头更换.png') {
            imgUrls.push(g.imgUrl)
          }
        })
      }
      return downloadImages(imgUrls).then(function(localUrls) {
        var urlIndex = 0
        if (data.items) {
          data.items.forEach(function(g) {
            if (g.imgUrl && g.imgUrl !== '/images/仁杆图片.png' && g.imgUrl !== '/images/皮头更换.png') {
              g.imgUrl = localUrls[urlIndex++] || g.imgUrl
            }
          })
        }
        return data
      })
    }).then(data => {
      wx.hideLoading()
      this.setData({
        orderDetail: data,
        statusText: data.statusText,
        orderTypeText: data.orderTypeText
      })
      // 超时时长：优先用后端返回的，兜底前端默认值
      const payTimeoutMinutes = data.payTimeoutMinutes || 15
      const receiveTimeoutMinutes = data.receiveTimeoutMinutes || 10080
      // 未付款：显示自动取消倒计时
      if (data.status === 0 && data.createTime) {
        this.startCountdown(data.createTime, payTimeoutMinutes, 'countdown')
      }
      // 待收货：显示自动收货倒计时
      if (data.status === 2 && data.shipTime) {
        this.startCountdown(data.shipTime, receiveTimeoutMinutes, 'receiveCountdown')
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  /**
   * 启动倒计时
   * @param {string} startTime 起始时间
   * @param {number} timeoutMinutes 超时时长（分钟）
   * @param {string} field 数据字段名（countdown / receiveCountdown）
   */
  startCountdown: function (startTime, timeoutMinutes, field) {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer)
    }
    const self = this
    // 健壮的日期解析（支持 ISO 8601 和多种格式）
    const parseDate = (timeStr) => {
      if (!timeStr) return null
      let d = new Date(timeStr)
      if (!isNaN(d.getTime())) return d
      d = new Date(timeStr.replace(/-/g, '/'))
      if (!isNaN(d.getTime())) return d
      return null
    }
    const updateCountdown = () => {
      const startDate = parseDate(startTime)
      if (!startDate) {
        self.setData({ [field]: '' })
        clearInterval(self.data.countdownTimer)
        return
      }
      const deadline = new Date(startDate.getTime() + timeoutMinutes * 60 * 1000)
      const diff = deadline - new Date()
      if (diff <= 0) {
        self.setData({ [field]: field === 'countdown' ? '订单已超时，请刷新页面' : '已超时自动收货' })
        clearInterval(self.data.countdownTimer)
        return
      }
      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
      const seconds = Math.floor((diff % (1000 * 60)) / 1000)
      let text = ''
      if (days > 0) {
        text = `${days}天${hours}时${minutes}分${seconds}秒`
      } else if (hours > 0) {
        text = `${hours}时${minutes}分${seconds}秒`
      } else if (minutes > 0) {
        text = `${minutes}分${seconds}秒`
      } else {
        text = `${seconds}秒`
      }
      self.setData({ [field]: text })
    }
    updateCountdown()
    const timer = setInterval(updateCountdown, 1000)
    this.setData({ countdownTimer: timer })
  },

  goBack: function () {
    wx.navigateBack()
  },

  payOrder: function () {
    const id = this.data.orderDetail.id
    const that = this
    wx.showModal({
      title: '提示',
      content: '确认支付该订单吗？',
      success: function (res) {
        if (res.confirm) {
          wx.showLoading({ title: '支付中...' })
          post('/api/order/pay/' + id).then(data => {
            wx.hideLoading()
            wx.showToast({ title: '支付成功', icon: 'success' })
            that.loadDetail(id)
          }).catch(() => {
            wx.hideLoading()
          })
        }
      }
    })
  },

  cancelOrder: function () {
    const id = this.data.orderDetail.id
    const that = this
    wx.showModal({
      title: '提示',
      content: '确定取消该订单吗？',
      success: function (res) {
        if (res.confirm) {
          post('/api/order/cancel/' + id).then(data => {
            wx.showToast({ title: '已取消', icon: 'success' })
            that.loadDetail(id)
          }).catch(() => {})
        }
      }
    })
  },

  confirmOrder: function () {
    const id = this.data.orderDetail.id
    const that = this
    wx.showModal({
      title: '提示',
      content: '确认已收到商品吗？',
      success: function (res) {
        if (res.confirm) {
          post('/api/order/confirm/' + id).then(data => {
            wx.showToast({ title: '已确认收货', icon: 'success' })
            that.loadDetail(id)
          }).catch(() => {})
        }
      }
    })
  },

  deleteOrder: function () {
    const id = this.data.orderDetail.id
    const that = this
    wx.showModal({
      title: '提示',
      content: '确定删除该订单吗？',
      success: function (res) {
        if (res.confirm) {
          post('/api/order/delete/' + id).then(data => {
            wx.showToast({ title: '已删除', icon: 'success' })
            wx.navigateBack()
          }).catch(() => {})
        }
      }
    })
  },

  applyRefund: function () {
    const id = this.data.orderDetail.id
    const orderType = this.data.orderDetail.orderType
    const status = this.data.orderDetail.status
    const that = this
    if (status === 1 && orderType === 1) {
      wx.showModal({
        title: '申请退款',
        content: '确定申请退款吗？（该订单可直接退款）',
        success: function (res) {
          if (res.confirm) {
            post('/api/order/refund/' + id).then(data => {
              wx.showToast({ title: '退款成功', icon: 'success' })
              that.loadDetail(id)
            }).catch(err => {})
          }
        }
      })
    } else {
      wx.showModal({
        title: '申请退款',
        content: '申请退款请截图订单联系客服微信',
        confirmText: '知道了',
        showCancel: false
      })
    }
  }
})