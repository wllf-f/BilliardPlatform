// pages/product-detail/product-detail.js
const { get, post, checkLogin } = require('../../utils/request.js')
const { downloadImages, encodeImageUrl, preparePreviewUrls } = require('../../utils/image.js')

Page({
  data: {
    id: '',
    detail: null,
    collected: false,
    loading: true,
    hideBuy: false
  },

  onLoad: function (options) {
    this.setData({ id: options.id, hideBuy: options.hideBuy === 'true' })
    this.loadDetail()
    this.checkCollect()
  },

  loadDetail: function () {
    var self = this
    wx.showLoading({ title: '加载中...' })
    get('/api/product/detail/' + this.data.id).then(res => {
      // 1. 处理 detailImgs：字符串拆数组 + 兜底 mainImg
      if (res.detailImgs && typeof res.detailImgs === 'string') {
        res.detailImgs = res.detailImgs.split(',').filter(Boolean)
      }
      if (!res.detailImgs || res.detailImgs.length === 0) {
        res.detailImgs = [res.mainImg]
      }

      // 2. 先统一用 encodeImageUrl 兜底（防止下载失败时直接引用的编码问题）
      res.detailImgs = res.detailImgs.map(u => encodeImageUrl(u) || u)
      if (res.mainImg) res.mainImg = encodeImageUrl(res.mainImg) || res.mainImg

      // 3. 构建 specRows：只显示有值的规格字段
      var isTip = false;
      var brand = (res.brandName || '');
      if (brand.indexOf('皮头') > -1) isTip = true;

      var specRows = [];
      if (res.cueType) specRows.push({ key: '类型', value: res.cueType });
      if (res.tipDiameter) specRows.push({ key: isTip ? '皮头直径' : '杆头直径', value: res.tipDiameter + ' mm' });
      if (res.weight) specRows.push({ key: '重量', value: res.weight + ' g' });
      if (res.hardness) specRows.push({ key: '硬度', value: res.hardness });
      if (res.material) specRows.push({ key: '材质', value: res.material });
      if (res.spec) specRows.push({ key: '规格', value: res.spec });
      if (res.applicableScene) specRows.push({ key: '适用场景', value: res.applicableScene });
      // 解析 params JSON 并追加（兼容两种格式：数组 [{key,value}] 和对象 {名称:值}）
      if (res.params) {
        try {
          var extra = typeof res.params === 'string' ? JSON.parse(res.params) : res.params;
          if (Array.isArray(extra)) {
            extra.forEach(function(p) {
              if (p && p.key && p.value) specRows.push({ key: p.key, value: p.value });
            });
          } else if (extra && typeof extra === 'object') {
            Object.keys(extra).forEach(function(k) {
              var v = extra[k];
              if (v !== null && v !== undefined && v !== '') {
                specRows.push({ key: k, value: String(v) });
              }
            });
          }
        } catch (e) {}
      }
      res.specRows = specRows;

      // 4. 立即 setData 一次（让页面能先显示，避免白屏）
      self.setData({ detail: res, loading: false })
      wx.setNavigationBarTitle({ title: res.name || '商品详情' })
      wx.hideLoading()

      // 4. 异步下载图片到本地临时路径（下载成功后更新）
      downloadImages(res.detailImgs).then(function(localUrls) {
        // 过滤下载失败的（本地路径一般以 wxfile:// 开头）
        var urls = res.detailImgs.map(function(original, i) {
          if (localUrls[i] && localUrls[i].indexOf('wxfile://') === 0) {
            return localUrls[i]
          }
          return original
        })
        // mainImg 取第一张
        if (urls.length > 0) res.mainImg = urls[0]
        res.detailImgs = urls
        self.setData({ detail: res })
      })
    }).catch(err => {
      wx.hideLoading()
      self.setData({ loading: false })
      wx.showToast({ title: '商品加载失败', icon: 'none' })
    })
  },

  checkCollect: function () {
    const token = wx.getStorageSync('token')
    if (!token) return
    get('/api/collection/check/' + this.data.id).then(res => {
      this.setData({ collected: res.collected })
    }).catch(() => {})
  },

  toggleCollect: function () {
    if (!checkLogin()) return
    post('/api/collection/toggle/' + this.data.id).then(res => {
      this.setData({ collected: res.collected })
      wx.showToast({
        title: res.collected ? '已收藏' : '已取消收藏',
        icon: 'none'
      })
    }).catch(() => {})
  },

  goBuy: function () {
    if (!checkLogin()) return
    wx.navigateTo({ url: '/pages/order/order?productId=' + this.data.id })
  },

  goHome: function () {
    wx.switchTab({ url: '/pages/index/index' })
  },

  previewImg: function (e) {
    const currentUrl = e.currentTarget.dataset.url
    const urls = this.data.detail.detailImgs || []
    var self = this
    // 先尝试预览（已是本地路径会直接显示），失败再重新下载
    wx.previewImage({ current: currentUrl, urls: urls, fail: function() {
      wx.showLoading({ title: '加载预览中...' })
      preparePreviewUrls(urls).then(function(localUrls) {
        wx.hideLoading()
        var newIdx = urls.indexOf(currentUrl)
        var current = (newIdx >= 0 && localUrls[newIdx]) ? localUrls[newIdx] : localUrls[0]
        self.setData({ 'detail.detailImgs': localUrls })
        wx.previewImage({ current: current, urls: localUrls })
      })
    }})
  },

  onBannerImgError: function (e) {
    var idx = e.currentTarget.dataset.idx
    var url = e.currentTarget.dataset.url
    console.error('[轮播图加载失败] index=' + idx + ', url=' + url)
  },

  onDetailImgError: function (e) {
    var idx = e.currentTarget.dataset.idx
    var url = e.currentTarget.dataset.url
    console.error('[详情图加载失败] index=' + idx + ', url=' + url)
  }
})
