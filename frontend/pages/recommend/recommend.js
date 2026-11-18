// pages/recommend/recommend.js
const { get, post } = require('../../utils/request.js')
const { downloadListImages, downloadImage, encodeImageUrl } = require('../../utils/image.js')

Page({
  data: {
    step: 1,
    recType: '',
    // 自定义导航栏尺寸（onLoad 中按真机胶囊按钮精确计算）
    statusBarHeight: 20,
    navBarHeight: 44,
    navTotalHeight: 64,
    // 球杆推荐：问卷参数（playType/level/powerType/likeType + 预算区间）
    cueForm: {
      playType: '准度防守',
      level: '中级',
      powerType: '均匀发力',
      likeType: '攻守均衡',
      minPrice: '',
      maxPrice: ''
    },
    // 皮头推荐：保留旧参数
    tipForm: {
      hardness: '均衡',
      tipType: '小头杆'
    },
    minPrice: '',
    maxPrice: '',
    resultList: [],
    loading: false
  },

  onLoad: function () {
    // 计算自定义导航栏高度，使内容区与右上角胶囊按钮垂直居中对齐
    try {
      const sys = (wx.getWindowInfo && wx.getWindowInfo()) || wx.getSystemInfoSync()
      const menu = wx.getMenuButtonBoundingClientRect()
      const statusBarHeight = sys.statusBarHeight || 20
      // 微信官方公式：内容区高度 = 胶囊距状态栏间距*2 + 胶囊高度
      const navBarHeight = (menu.top - statusBarHeight) * 2 + menu.height
      this.setData({
        statusBarHeight: statusBarHeight,
        navBarHeight: navBarHeight || 44,
        navTotalHeight: statusBarHeight + (navBarHeight || 44)
      })
    } catch (e) {
      // 异常时用默认值
    }
  },

  /**
   * 自定义导航栏左上角返回按钮：
   * - step 3（结果页）：回到 step 2 问卷页，保留已填参数方便微调后重新推荐
   * - step 2（问卷页）：回到 step 1 类型选择页
   * - step 1（类型页）：退出当前页面，返回上一页
   */
  navBack: function () {
    const step = this.data.step
    if (step === 3) {
      this.setData({ step: 2, resultList: [], loading: false })
    } else if (step === 2) {
      this.setData({ step: 1 })
    } else {
      wx.navigateBack({ delta: 1, fail: function () {
        wx.switchTab({ url: '/pages/index/index' })
      }})
    }
  },

  selectType: function (e) {
    this.setData({ recType: e.currentTarget.dataset.type, step: 2 })
  },

  backStep: function () {
    this.setData({ step: 1 })
  },

  setCueField: function (e) {
    const field = e.currentTarget.dataset.field
    const val = e.currentTarget.dataset.val
    this.setData({ ['cueForm.' + field]: val })
  },

  setCueInput: function (e) {
    const field = e.currentTarget.dataset.field
    this.setData({ ['cueForm.' + field]: e.detail.value })
  },

  setTipField: function (e) {
    const field = e.currentTarget.dataset.field
    const val = e.currentTarget.dataset.val
    this.setData({ ['tipForm.' + field]: val })
  },

  setTipInput: function (e) {
    const field = e.currentTarget.dataset.field
    this.setData({ ['tipForm.' + field]: e.detail.value })
  },

  onInput: function (e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  submit: function () {
    const { recType, cueForm, tipForm, minPrice, maxPrice } = this.data
    this.setData({ loading: true, step: 3 })
    wx.showLoading({ title: '推荐中...' })

    var that = this

    if (recType === 'cue') {
      const params = {
        playType: cueForm.playType,
        level: cueForm.level,
        powerType: cueForm.powerType,
        likeType: cueForm.likeType,
        minPrice: cueForm.minPrice ? Number(cueForm.minPrice) : null,
        maxPrice: cueForm.maxPrice ? Number(cueForm.maxPrice) : null
      }
      post('/api/recommend/cue', params).then(function(res) {
        wx.hideLoading()
        var list = res || []
        // 立即显示结果（使用编码后的远程 URL），让列表先渲染
        that.setData({ resultList: list, loading: false })
        // 后台下载图片到本地临时路径，完成后更新显示
        return downloadListImages(list, 'mainImg')
      }).then(function(list) {
        // 下载完成，更新为本地路径
        // 仅更新那些已成功下载的条目
        var curList = that.data.resultList.slice()
        var updated = false
        for (var i = 0; i < list.length && i < curList.length; i++) {
          if (list[i].mainImg && curList[i].mainImg !== list[i].mainImg) {
            curList[i] = Object.assign({}, curList[i], { mainImg: list[i].mainImg })
            updated = true
          }
        }
        if (updated) {
          that.setData({ resultList: curList })
        }
      }).catch(function() {
        wx.hideLoading()
        that.setData({ loading: false })
      })
    } else {
      // 皮头推荐：根据杆型确定直径区间
      // 小头杆皮头直径通常 10-12mm，大头杆皮头直径通常 12-14mm
      var isSmall = tipForm.tipType === '小头杆'
      const params = {
        type: 'tip',
        hardness: tipForm.hardness,
        headType: tipForm.tipType,
        minTip: isSmall ? '10' : '12',
        maxTip: isSmall ? '12' : '14',
        minPrice: minPrice,
        maxPrice: maxPrice
      }
      get('/api/recommend/list', params).then(function(res) {
        wx.hideLoading()
        var list = res || []
        that.setData({ resultList: list, loading: false })
        return downloadListImages(list, 'mainImg')
      }).then(function(list) {
        var curList = that.data.resultList.slice()
        var updated = false
        for (var i = 0; i < list.length && i < curList.length; i++) {
          if (list[i].mainImg && curList[i].mainImg !== list[i].mainImg) {
            curList[i] = Object.assign({}, curList[i], { mainImg: list[i].mainImg })
            updated = true
          }
        }
        if (updated) {
          that.setData({ resultList: curList })
        }
      }).catch(function() {
        wx.hideLoading()
        that.setData({ loading: false })
      })
    }
  },

  resetAll: function () {
    this.setData({
      step: 1,
      recType: '',
      resultList: [],
      minPrice: '',
      maxPrice: ''
    })
  },

  goDetail: function (e) {
    wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + e.currentTarget.dataset.id })
  },

  /**
   * 图片加载失败重试
   * 当 wx.downloadListImages 下载失败（如超时），
   * <image> 组件收到的是编码后的远程 URL，部分机型渲染失败。
   * 此函数捕获 binderror 事件，再次调用 downloadImage 重试下载。
   */
  onImgError: function (e) {
    var url = e.currentTarget.dataset.url
    var index = e.currentTarget.dataset.index
    var that = this
    if (!url || !this.data.resultList[index]) return

    // 避免死循环：已经是 wxfile:// 临时路径的不再重试
    if (url.indexOf('wxfile://') === 0) return

    downloadImage(url).then(function (localPath) {
      var list = that.data.resultList.slice()
      if (list[index] && localPath && localPath !== url) {
        list[index] = Object.assign({}, list[index], { mainImg: localPath })
        that.setData({ resultList: list })
      }
    })
  }
})
