import { API_BASE_URL } from "@/config/env";

const toQueryString = (params) => {
  const searchParams = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    // 与后端约定：忽略 空值，避免发送空字符串污染查询参数。
    if (value !== undefined && value !== null) {
      searchParams.append(key, String(value));
    }
  });
  return searchParams.toString();
};

const buildUrl = (path, params) => {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const url = `${API_BASE_URL}${normalizedPath}`;
  if (!params || Object.keys(params).length === 0) return url;
  return `${url}?${toQueryString(params)}`;
};

export const apiRequest = async (path, options = {}) => {
  const {
    method = "GET",
    query,
    data,
    token,
    headers = {},
    responseType = "json",
  } = options;

  const upperMethod = method.toUpperCase();
  // GET 请求参数拼接到地址栏；非 GET 请求的数据放入请求体。
  const url = buildUrl(path, upperMethod === "GET" ? data || query : query);

  const requestHeaders = {
    ...headers,
  };

  let body;
  if (upperMethod !== "GET" && data) {
    if (data instanceof FormData) {
      // 表单数据让浏览器自动设置 multipart 边界。
      body = data;
    } else {
      // 与后端表单接口兼容：x-www-form-urlencoded。
      body = toQueryString(data);
      requestHeaders["Content-Type"] = "application/x-www-form-urlencoded;charset=UTF-8";
    }
  }

  if (token) {
    requestHeaders.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    method: upperMethod,
    headers: requestHeaders,
    body,
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  if (responseType === "text") {
    return response.text();
  }

  return response.json();
};
