// pages/order-list/order-list.js
const { get, post } = require('../../utils/request.js')
const { downloadImages, clearImageCache } = require('../../utils/image.js')

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

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    total: 0,
    currentStatus: '',
    loading: false,
    noMore: false,
    countdownTimers: {}
  },

  onLoad: function (options) {
    if (options.status !== undefined && options.status !== '') {
      this.setData({ currentStatus: options.status })
    }
    this.loadList(true)
  },

  onShow: function () {
    if (this.data.list.length > 0) {
      this.loadList(true)
    }
  },

  onUnload: function () {
    this.clearAllTimers()
    clearImageCache()
  },

  onHide: function () {
    // 切后台/切换标签页释放图片缓存，避免长会话内存增长
    clearImageCache()
  },

  /** 清理所有倒计时定时器 */
  clearAllTimers: function () {
    const timers = this.data.countdownTimers
    for (const key in timers) {
      clearInterval(timers[key])
    }
    this.setData({ countdownTimers: {} })
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

  changeStatus: function (e) {
    const status = e.currentTarget.dataset.status
    this.setData({ currentStatus: status })
    this.loadList(true)
  },

  loadList: function (reset) {
    if (this.data.loading) return Promise.resolve()
    const page = reset ? 1 : this.data.page + 1
    this.setData({ loading: true, page: page })
    if (reset) {
      // 重置前先清理旧的倒计时定时器，避免累积泄漏
      this.clearAllTimers()
      this.setData({ noMore: false, list: [] })
    }
    const params = {
      page: page,
      size: this.data.size,
      status: this.data.currentStatus
    }
    var total = 0
    return get('/api/order/list', params).then(res => {
      total = res.total || 0
      var records = (res.records || []).map(item => {
        item.statusText = STATUS_MAP[item.status] || '未知'
        item.orderTypeText = TYPE_MAP[item.orderType] || '普通商品'
        // 用 createTimeDisplay 展示（去掉 T），保留原始 createTime 用于倒计时计算
        item.createTimeDisplay = item.createTime ? String(item.createTime).replace('T', ' ') : item.createTime
        item.shipTimeDisplay = item.shipTime ? String(item.shipTime).replace('T', ' ') : item.shipTime
        item.itemCount = (item.items || []).reduce((sum, g) => sum + (g.quantity || 0), 0)
        // 超时时长：优先用后端返回的配置，兜底前端默认值
        item.payTimeoutMinutes = item.payTimeoutMinutes || 15
        item.receiveTimeoutMinutes = item.receiveTimeoutMinutes || 10080
        // 未付款订单：显示自动取消倒计时
        if (item.status === 0 && item.createTime) {
          item.countdown = this.calcCountdown(item.createTime, item.payTimeoutMinutes)
          this.startItemCountdown(item.id, item.createTime, item.payTimeoutMinutes, 'countdown', 0)
        }
        // 待收货订单：显示自动收货倒计时
        if (item.status === 2 && item.shipTime) {
          item.receiveCountdown = this.calcCountdown(item.shipTime, item.receiveTimeoutMinutes)
          this.startItemCountdown(item.id, item.shipTime, item.receiveTimeoutMinutes, 'receiveCountdown', 2)
        }
        // 给定制/更换订单添加默认商品图片
        if (item.orderType === 2 || item.orderType === 3) {
          if (!item.items || item.items.length === 0) {
            item.items = [{
              productName: item.orderType === 2 ? '仁杆定制' : '皮头更换',
              price: item.totalAmount,
              quantity: 1,
              subtotal: item.totalAmount,
              imgUrl: ''
            }]
          }
          // 服务类订单：优先使用本地 PNG 图（后端 SVG 在微信小程序中兼容性差）
          const localImg = item.orderType === 2 ? '/images/仁杆图片.png' : '/images/皮头更换.png'
          item.items.forEach(g => {
            // 如果后端给的是 SVG 或空值，替换为本地图
            if (!g.imgUrl || g.imgUrl.indexOf('.svg') > -1 || g.imgUrl.indexOf('service/') > -1) {
              g.imgUrl = localImg
            }
          })
        }
        return item
      })
      // 收集所有商品图片URL并下载
      var allImgUrls = []
      records.forEach(function(item) {
        if (item.items) {
          item.items.forEach(function(g) {
            // 只收集非本地默认图片的URL
            if (g.imgUrl && g.imgUrl !== '/images/仁杆图片.png' && g.imgUrl !== '/images/皮头更换.png') {
              allImgUrls.push(g.imgUrl)
            }
          })
        }
      })
      return downloadImages(allImgUrls).then(function(localUrls) {
        // 替换为本地路径
        var urlIndex = 0
        records.forEach(function(item) {
          if (item.items) {
            item.items.forEach(function(g) {
              // 只替换非本地默认图片
              if (g.imgUrl && g.imgUrl !== '/images/仁杆图片.png' && g.imgUrl !== '/images/皮头更换.png') {
                g.imgUrl = localUrls[urlIndex++] || g.imgUrl
              }
            })
          }
        })
        return records
      })
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

  // 健壮的日期解析（支持 ISO 8601 和多种格式）
  parseDate: function (timeStr) {
    if (!timeStr) return null
    let d = new Date(timeStr)
    if (!isNaN(d.getTime())) return d
    // 尝试替换 - 为 /（兼容旧格式）
    d = new Date(timeStr.replace(/-/g, '/'))
    if (!isNaN(d.getTime())) return d
    console.warn('日期解析失败:', timeStr)
    return null
  },
  /**
   * 计算倒计时
   * @param {string} startTimeStr 起始时间（下单时间/发货时间）
   * @param {number} timeoutMinutes 超时时长（分钟）
   * @returns {string} 倒计时文本，如 "15分0秒" 或 "2天3时15分0秒" 或 "已超时"
   */
  calcCountdown: function (startTimeStr, timeoutMinutes) {
    const startDate = this.parseDate(startTimeStr)
    if (!startDate) return ''
    const deadline = new Date(startDate.getTime() + timeoutMinutes * 60 * 1000)
    const now = new Date()
    const diff = deadline - now
    if (diff <= 0) {
      return '已超时'
    }
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)
    if (days > 0) {
      return `${days}天${hours}时${minutes}分${seconds}秒`
    } else if (hours > 0) {
      return `${hours}时${minutes}分${seconds}秒`
    } else if (minutes > 0) {
      return `${minutes}分${seconds}秒`
    } else {
      return `${seconds}秒`
    }
  },

  /**
   * 启动单条订单倒计时
   * @param {number} orderId 订单ID
   * @param {string} startTimeStr 起始时间
   * @param {number} timeoutMinutes 超时时长（分钟）
   * @param {string} field 要更新的字段名（countdown / receiveCountdown）
   * @param {number} expectStatus 期望的订单状态（0未付款 / 2待收货）
   */
  startItemCountdown: function (orderId, startTimeStr, timeoutMinutes, field, expectStatus) {
    const updateCountdown = () => {
      const list = this.data.list
      let targetIndex = -1
      for (let i = 0; i < list.length; i++) {
        if (list[i].id === orderId) {
          targetIndex = i
          break
        }
      }
      if (targetIndex < 0) return
      const item = list[targetIndex]
      // 状态不匹配则停止倒计时
      if (item.status !== expectStatus) return
      const countdown = this.calcCountdown(startTimeStr, timeoutMinutes)
      if (countdown === '已超时') {
        clearInterval(this.data.countdownTimers[orderId + '_' + field])
        delete this.data.countdownTimers[orderId + '_' + field]
      }
      this.setData({ ['list[' + targetIndex + '].' + field]: countdown })
    }
    updateCountdown()
    const timer = setInterval(updateCountdown, 1000)
    const timers = this.data.countdownTimers
    const key = orderId + '_' + field
    timers[key] = timer
    this.setData({ countdownTimers: timers })
  },

  payOrder: function (e) {
    const id = e.currentTarget.dataset.id
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
            that.loadList(true)
          }).catch(() => {
            wx.hideLoading()
          })
        }
      }
    })
  },

  cancelOrder: function (e) {
    const id = e.currentTarget.dataset.id
    const that = this
    wx.showModal({
      title: '提示',
      content: '确定取消该订单吗？',
      success: function (res) {
        if (res.confirm) {
          post('/api/order/cancel/' + id).then(data => {
            wx.showToast({ title: '已取消', icon: 'success' })
            that.loadList(true)
          }).catch(() => {})
        }
      }
    })
  },

  confirmOrder: function (e) {
    const id = e.currentTarget.dataset.id
    const that = this
    wx.showModal({
      title: '提示',
      content: '确认已收到商品吗？',
      success: function (res) {
        if (res.confirm) {
          post('/api/order/confirm/' + id).then(data => {
            wx.showToast({ title: '已确认收货', icon: 'success' })
            that.loadList(true)
          }).catch(() => {})
        }
      }
    })
  },

  deleteOrder: function (e) {
    const id = e.currentTarget.dataset.id
    const that = this
    wx.showModal({
      title: '提示',
      content: '确定删除该订单吗？',
      success: function (res) {
        if (res.confirm) {
          post('/api/order/delete/' + id).then(data => {
            wx.showToast({ title: '已删除', icon: 'success' })
            that.loadList(true)
          }).catch(() => {})
        }
      }
    })
  },

  goDetail: function (e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/order-detail/order-detail?id=' + id
    })
  }
})
