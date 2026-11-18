// pages/category/category.js
const { get } = require('../../utils/request.js')

Page({
  data: {
    categoryList: [],
    brandList: [],
    activeIndex: 0,
    currentCategoryId: '',
    currentCategoryName: '',
    loading: true
  },

  onLoad: function () {
    this.loadCategory()
  },

  loadCategory: function () {
    get('/api/category/list').then(res => {
      const list = res || []
      this.setData({
        categoryList: list,
        loading: false
      })
      if (list.length > 0) {
        this.setData({
          currentCategoryId: list[0].id,
          currentCategoryName: list[0].name
        })
        this.loadBrand(list[0].id)
      }
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  loadBrand: function (categoryId) {
    get('/api/category/brand', { categoryId: categoryId }).then(res => {
      this.setData({ brandList: res || [] })
    }).catch(() => {
      this.setData({ brandList: [] })
    })
  },

  selectCategory: function (e) {
    const index = e.currentTarget.dataset.index
    const id = e.currentTarget.dataset.id
    const name = this.data.categoryList[index].name
    this.setData({
      activeIndex: index,
      currentCategoryId: id,
      currentCategoryName: name
    })
    this.loadBrand(id)
  },

  goProductListByBrand: function (e) {
    const brandId = e.currentTarget.dataset.brandId
    let url = '/pages/product-list/product-list?categoryId=' + this.data.currentCategoryId + '&categoryName=' + this.data.currentCategoryName
    if (brandId) {
      url += '&brandId=' + brandId
    }
    wx.navigateTo({ url: url })
  }
})
