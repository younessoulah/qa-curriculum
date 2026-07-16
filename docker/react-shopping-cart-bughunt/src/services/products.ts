import { IGetProductsResponse } from 'models';

export const getProducts = async () => {
  const response: IGetProductsResponse = require('static/json/products.json');

  const { products } = response.data || [];

  return products;
};
