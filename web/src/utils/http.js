import { API_BASE_URL } from "@/config/env";

/**
 * Handles toQueryString.
 * ??toQueryString?
 */
const toQueryString = (params) => {
  const searchParams = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      searchParams.append(key, String(value));
    }
  });
  return searchParams.toString();
};

/**
 * Handles buildUrl.
 * ??buildUrl?
 */
const buildUrl = (path, params) => {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const url = `${API_BASE_URL}${normalizedPath}`;
  if (!params || Object.keys(params).length === 0) return url;
  return `${url}?${toQueryString(params)}`;
};

/**
 * Handles handleUnauthorized.
 * ??handleUnauthorized?
 */
const handleUnauthorized = () => {
  localStorage.removeItem("jwt_token");
  const pathname = window.location.pathname || "";
  const isAuthPage =
    pathname.startsWith("/user/account/login") || pathname.startsWith("/user/account/register");

  if (!isAuthPage) {
    window.location.assign("/user/account/login/");
  }
};

export const apiRequest = async (path, options = {}) => {
  const {
    method = "GET",
    query,
    data,
    token,
    headers = {},
    payloadType = "form",
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
    } else if (payloadType === "json") {
      body = JSON.stringify(data);
      requestHeaders["Content-Type"] = "application/json;charset=UTF-8";
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
    if (response.status === 401) {
      handleUnauthorized();
      const unauthorizedError = new Error("Unauthorized");
      unauthorizedError.status = 401;
      throw unauthorizedError;
    }
    throw new Error(`HTTP ${response.status}`);
  }

  if (responseType === "text") {
    return response.text();
  }

  return response.json();
};

