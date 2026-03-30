import { API_BASE_URL } from "@/config/env";

const toQueryString = (params) => {
  const searchParams = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
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
  const url = buildUrl(path, upperMethod === "GET" ? data || query : query);

  const requestHeaders = {
    ...headers,
  };

  let body;
  if (upperMethod !== "GET" && data) {
    if (data instanceof FormData) {
      body = data;
    } else {
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
