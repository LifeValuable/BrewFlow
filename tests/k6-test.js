import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';

// ============================================================================
// КОНФИГУРАЦИЯ
// ============================================================================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Настройка RPS через environment variables
const TOTAL_RPS = parseInt(__ENV.TOTAL_RPS) || 400;
const TEST_DURATION = __ENV.TEST_DURATION || '5m';
// Распределение нагрузки (в процентах)
const CATALOG_PERCENT = parseFloat(__ENV.CATALOG_PERCENT) || 85;
const SHOP_PERCENT = parseFloat(__ENV.SHOP_PERCENT) || 8;
const CHECK_ORDERS_PERCENT = parseFloat(__ENV.CHECK_ORDERS_PERCENT) || 5;
const REGISTER_PERCENT = parseFloat(__ENV.REGISTER_PERCENT) || 2;

// Рассчитываем RPS для каждого сценария
const CATALOG_RPS = Math.round(TOTAL_RPS * CATALOG_PERCENT / 100);
const SHOP_RPS = Math.round(TOTAL_RPS * SHOP_PERCENT / 100);
const CHECK_ORDERS_RPS = Math.round(TOTAL_RPS * CHECK_ORDERS_PERCENT / 100);
const REGISTER_RPS = Math.round(TOTAL_RPS * REGISTER_PERCENT / 100);

// Кастомные метрики
const loginSuccessRate = new Rate('login_success_rate');
const orderCreationRate = new Rate('order_creation_rate');
const catalogViewsCounter = new Counter('catalog_views');
const cartOperationsCounter = new Counter('cart_operations');
const orderCreationTime = new Trend('order_creation_duration');
const actualLoginsCounter = new Counter('actual_logins_performed');
const outOfStockErrors = new Counter('out_of_stock_errors');
const emptyCartAttempts = new Counter('empty_cart_order_attempts');
const userContentionCounter = new Counter('user_contention_events');

// Данные для тестовых пользователей (увеличено для лучшей изоляции)
const USER_COUNT = parseInt(__ENV.USER_COUNT) || 200;
const testUsers = new SharedArray('users', function() {
  const users = [];
  for (let i = 0; i < USER_COUNT; i++) {
    users.push({
      email: `loadtest_user_${i}@brewflow.test`,
      password: 'TestPassword123!',
      firstName: `TestUser${i}`,
      lastName: `LoadTest${i}`
    });
  }
  return users;
});

// ID продуктов из вашей базы
const PRODUCT_IDS = [
  '550e8400-e29b-41d4-a716-446655440001',
  '550e8400-e29b-41d4-a716-446655440002',
  '550e8400-e29b-41d4-a716-446655440003',
  '550e8400-e29b-41d4-a716-446655440004',
  '550e8400-e29b-41d4-a716-446655440005'
];

// Кеш токенов с блокировкой для предотвращения конкурентного использования
const tokenCache = {};
const userLocks = {};

// ============================================================================
// ПАРАМЕТРЫ НАГРУЗОЧНОГО ТЕСТИРОВАНИЯ
// ============================================================================

export const options = {
  scenarios: {
    catalog_browsers: {
      executor: 'ramping-arrival-rate',
      exec: 'browseCatalog',
      startRate: Math.max(1, Math.round(CATALOG_RPS * 0.02)),
      timeUnit: '1s',
      stages: [
        { duration: '30s', target: Math.round(CATALOG_RPS * 0.05) },
        { duration: '30s', target: Math.round(CATALOG_RPS * 0.10) },
        { duration: '1m', target: Math.round(CATALOG_RPS * 0.20) },
        { duration: '1m', target: Math.round(CATALOG_RPS * 0.40) },
        { duration: '1m30s', target: Math.round(CATALOG_RPS * 0.70) },
        { duration: '1m30s', target: CATALOG_RPS },
        { duration: TEST_DURATION, target: CATALOG_RPS },
        // Плавное снижение
        { duration: '1m', target: Math.round(CATALOG_RPS * 0.60) },
        { duration: '1m', target: Math.round(CATALOG_RPS * 0.30) },
        { duration: '30s', target: 0 },
      ],
      preAllocatedVUs: Math.round(CATALOG_RPS * 1.5),
      maxVUs: Math.round(CATALOG_RPS * 2.5),
    },

    active_shoppers: {
      executor: 'ramping-arrival-rate',
      exec: 'shopAndOrder',
      startRate: Math.max(1, Math.round(SHOP_RPS * 0.02)),
      timeUnit: '1s',
      stages: [
        { duration: '30s', target: Math.round(SHOP_RPS * 0.05) },
        { duration: '30s', target: Math.round(SHOP_RPS * 0.10) },
        { duration: '1m', target: Math.round(SHOP_RPS * 0.20) },
        { duration: '1m', target: Math.round(SHOP_RPS * 0.40) },
        { duration: '1m30s', target: Math.round(SHOP_RPS * 0.70) },
        { duration: '1m30s', target: SHOP_RPS },
        { duration: TEST_DURATION, target: SHOP_RPS },
        { duration: '1m', target: Math.round(SHOP_RPS * 0.60) },
        { duration: '1m', target: Math.round(SHOP_RPS * 0.30) },
        { duration: '30s', target: 0 },
      ],
      preAllocatedVUs: Math.round(SHOP_RPS * 2.5),
      maxVUs: Math.round(SHOP_RPS * 5),
    },

    order_checkers: {
      executor: 'ramping-arrival-rate',
      exec: 'checkOrders',
      startRate: Math.max(1, Math.round(CHECK_ORDERS_RPS * 0.02)),
      timeUnit: '1s',
      stages: [
        { duration: '30s', target: Math.round(CHECK_ORDERS_RPS * 0.05) },
        { duration: '30s', target: Math.round(CHECK_ORDERS_RPS * 0.10) },
        { duration: '1m', target: Math.round(CHECK_ORDERS_RPS * 0.20) },
        { duration: '1m', target: Math.round(CHECK_ORDERS_RPS * 0.40) },
        { duration: '1m30s', target: Math.round(CHECK_ORDERS_RPS * 0.70) },
        { duration: '1m30s', target: CHECK_ORDERS_RPS },
        { duration: TEST_DURATION, target: CHECK_ORDERS_RPS },
        { duration: '1m', target: Math.round(CHECK_ORDERS_RPS * 0.60) },
        { duration: '1m', target: Math.round(CHECK_ORDERS_RPS * 0.30) },
        { duration: '30s', target: 0 },
      ],
      preAllocatedVUs: Math.round(CHECK_ORDERS_RPS * 2.5),
      maxVUs: Math.round(CHECK_ORDERS_RPS * 5),
    },

    new_registrations: {
      executor: 'ramping-arrival-rate',
      exec: 'registerNewUser',
      startRate: 1,
      timeUnit: '1s',
      stages: [
        { duration: '30s', target: Math.max(1, Math.round(REGISTER_RPS * 0.10)) },
        { duration: '30s', target: Math.max(1, Math.round(REGISTER_RPS * 0.20)) },
        { duration: '1m', target: Math.max(2, Math.round(REGISTER_RPS * 0.40)) },
        { duration: '1m', target: Math.max(2, Math.round(REGISTER_RPS * 0.60)) },
        { duration: '1m30s', target: Math.max(3, Math.round(REGISTER_RPS * 0.85)) },
        { duration: '1m30s', target: REGISTER_RPS },
        { duration: TEST_DURATION, target: REGISTER_RPS },
        { duration: '1m', target: Math.max(2, Math.round(REGISTER_RPS * 0.50)) },
        { duration: '1m', target: 1 },
        { duration: '30s', target: 0 },
      ],
      preAllocatedVUs: Math.max(15, Math.round(REGISTER_RPS * 4)),
      maxVUs: Math.max(30, Math.round(REGISTER_RPS * 8)),
    },
  },

  thresholds: {
    'http_req_duration': ['p(95)<2000', 'p(99)<5000'],
    'http_req_failed': ['rate<0.10'],
    'login_success_rate': ['rate>0.85'],
    'order_creation_rate': ['rate>0.90'],
  },

  insecureSkipTLSVerify: true,
  noConnectionReuse: false,
  userAgent: 'K6-LoadTest-BrewFlow/1.0',
  gracefulStop: '30s',
  setupTimeout: '120s',
};

// ============================================================================
// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ============================================================================

function getRandomUser() {
  return testUsers[Math.floor(Math.random() * testUsers.length)];
}

function getRandomProduct() {
  return PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
}

function getRandomQuantity() {
  return Math.floor(Math.random() * 3) + 1;
}

function randomSleep(min, max) {
  sleep(Math.random() * (max - min) + min);
}

function getCachedToken(user) {
  const cacheKey = user.email;
  const now = Date.now();
  const TOKEN_TTL = 600000; // 10 минут
  const LOCK_DURATION = 8000; // 8 секунд - достаточно для завершения сценария
  
  // Проверяем, используется ли пользователь сейчас другим VU
  if (userLocks[cacheKey] && userLocks[cacheKey] > now) {
    userContentionCounter.add(1);
    return null; // Пользователь занят
  }
  
  // Проверяем кеш токенов
  if (tokenCache[cacheKey] && tokenCache[cacheKey].expiresAt > now) {
    // Блокируем пользователя на время выполнения операций
    userLocks[cacheKey] = now + LOCK_DURATION;
    return tokenCache[cacheKey].token;
  }
  
  // Токен протух или отсутствует - логинимся
  actualLoginsCounter.add(1);
  const token = login(user);
  
  if (token) {
    tokenCache[cacheKey] = {
      token: token,
      expiresAt: now + TOKEN_TTL
    };
    userLocks[cacheKey] = now + LOCK_DURATION;
  }
  
  return token;
}

function releaseLock(user) {
  const cacheKey = user.email;
  delete userLocks[cacheKey];
}

function registerUser(user) {
  const payload = JSON.stringify({
    email: user.email,
    password: user.password,
    firstName: user.firstName,
    lastName: user.lastName
  });

  const res = http.post(`${BASE_URL}/api/auth/register`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'Register' },
  });

  return check(res, {
    'registration successful': (r) => r.status === 200 || r.status === 201,
  });
}

function login(user) {
  const payload = JSON.stringify({
    email: user.email,
    password: user.password
  });

  const res = http.post(`${BASE_URL}/api/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'Login' },
  });

  const success = check(res, {
    'login successful': (r) => r.status === 200,
    'token received': (r) => r.json('jwtToken') !== undefined,
  });

  loginSuccessRate.add(success);

  if (success) {
    return res.json('jwtToken');
  }
  return null;
}

function getProducts(token) {
  const res = http.get(`${BASE_URL}/api/products`, {
    headers: token ? { 'Authorization': `Bearer ${token}` } : {},
    tags: { name: 'GetProducts' },
  });

  check(res, {
    'products loaded': (r) => r.status === 200,
    'products is array': (r) => Array.isArray(r.json()),
  });

  catalogViewsCounter.add(1);
  return res;
}

function addToCart(token, productId, quantity, maxRetries = 3) {
  let attempt = 0;
  
  while (attempt <= maxRetries) {
    const payload = JSON.stringify({
      productId: productId,
      quantity: quantity
    });

    const res = http.post(`${BASE_URL}/api/cart/items`, payload, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      tags: { name: 'AddToCart' },
    });

    if (res.status === 200 || res.status === 201) {
      cartOperationsCounter.add(1);
      return true;
    }
    
    // Обработка ошибок нехватки stock
    if (res.status === 409 || (res.status === 400 && res.body.includes('Insufficient stock'))) {
      outOfStockErrors.add(1);
      return false; // Не ретраим - товара действительно нет
    }
    
    // Retry на конфликты и временные ошибки с экспоненциальной задержкой
    if (res.status === 500 && attempt < maxRetries) {
      attempt++;
      const backoff = 0.1 * Math.pow(2, attempt); // 0.2s, 0.4s, 0.8s
      sleep(backoff);
      continue;
    }
    
    return false;
  }
  
  return false;
}

function getCart(token) {
  const res = http.get(`${BASE_URL}/api/cart`, {
    headers: { 'Authorization': `Bearer ${token}` },
    tags: { name: 'GetCart' },
  });

  check(res, {
    'cart loaded': (r) => r.status === 200,
  });

  return res;
}

function createOrder(token, maxRetries = 1) {
  let attempt = 0;
  
  while (attempt <= maxRetries) {
    const startTime = Date.now();

    const res = http.post(`${BASE_URL}/api/orders`, null, {
      headers: { 'Authorization': `Bearer ${token}` },
      tags: { name: 'CreateOrder' },
    });

    const duration = Date.now() - startTime;
    orderCreationTime.add(duration);

    const success = check(res, {
      'order created': (r) => r.status === 200 || r.status === 201,
      'order has id': (r) => r.json('id') !== undefined,
    });

    // Отслеживаем различные типы ошибок
    if (res.status === 400) {
      const body = res.body;
      
      if (body.includes('insufficient') || body.includes('Insufficient stock')) {
        outOfStockErrors.add(1);
        orderCreationRate.add(false);
        return false; // Не ретраим - нет товара
      }
      
      if (body.includes('empty') || body.includes('Empty cart')) {
        emptyCartAttempts.add(1);
        // Retry один раз на случай задержки синхронизации
        if (attempt < maxRetries) {
          attempt++;
          sleep(0.3);
          continue;
        }
        orderCreationRate.add(false);
        return false;
      }
    }

    orderCreationRate.add(success);
    return success;
  }
  
  orderCreationRate.add(false);
  return false;
}

function getOrdersHistory(token) {
  const res = http.get(`${BASE_URL}/api/orders`, {
    headers: { 'Authorization': `Bearer ${token}` },
    tags: { name: 'GetOrdersHistory' },
  });

  check(res, {
    'orders history loaded': (r) => r.status === 200,
  });

  return res;
}

// ============================================================================
// СЦЕНАРИИ ТЕСТИРОВАНИЯ
// ============================================================================

export function browseCatalog() {
  group('Browse Catalog', () => {
    getProducts(null); // Анонимный доступ
    randomSleep(0.5, 1.5);
  });
}

export function shopAndOrder() {
  let user = null;
  let token = null;
  
  group('Shop and Order', () => {
    // Пытаемся получить свободного пользователя
    let attempts = 0;
    const maxAttempts = 5;
    
    while (!token && attempts < maxAttempts) {
      user = getRandomUser();
      token = getCachedToken(user);
      
      if (!token) {
        attempts++;
        sleep(0.05 * attempts); // Небольшая экспоненциальная задержка
      }
    }
    
    if (!token) {
      // Все пользователи заняты - пропускаем итерацию
      return;
    }
    
    try {
      randomSleep(0.2, 0.5);

      getProducts(token);
      randomSleep(1, 2);

      // Добавляем товары в корзину
      let successfullyAdded = 0;
      const itemsCount = Math.floor(Math.random() * 2) + 1;
      
      for (let i = 0; i < itemsCount; i++) {
        const added = addToCart(token, getRandomProduct(), getRandomQuantity(), 3);
        if (added) {
          successfullyAdded++;
          randomSleep(0.5, 1);
        } else {
          randomSleep(0.2, 0.5);
        }
      }

      // Создаём заказ только если успешно добавили товары
      // НЕ проверяем корзину отдельно - избегаем race condition
      if (Math.random() < 0.75 && successfullyAdded > 0) {
        createOrder(token);
        randomSleep(0.5, 1);
      }
    } finally {
      // ВАЖНО: Освобождаем блокировку пользователя
      if (user) {
        releaseLock(user);
      }
    }
  });
}

export function checkOrders() {
  let user = null;
  let token = null;
  
  group('Check Orders', () => {
    // Получаем свободного пользователя
    let attempts = 0;
    const maxAttempts = 3;
    
    while (!token && attempts < maxAttempts) {
      user = getRandomUser();
      token = getCachedToken(user);
      
      if (!token) {
        attempts++;
        sleep(0.05 * attempts);
      }
    }
    
    if (!token) return;
    
    try {
      randomSleep(0.2, 0.5);
      getOrdersHistory(token);
      randomSleep(0.5, 1);
    } finally {
      if (user) {
        releaseLock(user);
      }
    }
  });
}

export function registerNewUser() {
  const timestamp = Date.now();
  const randomStr = Math.random().toString(36).substr(2, 6);
  const newUser = {
    email: `lt_${timestamp}_${__VU}_${randomStr}@test.com`,
    password: 'TestPassword123!',
    firstName: `User${__VU}`,
    lastName: `Test${timestamp}`
  };

  group('New Registration', () => {
    const registered = registerUser(newUser);
    randomSleep(0.3, 0.8);

    if (registered) {
      const token = login(newUser);
      if (token) {
        getProducts(token);
        randomSleep(1, 2);
      }
    }
  });
}

// ============================================================================
// LIFECYCLE HOOKS
// ============================================================================

export function setup() {
  console.log('='.repeat(80));
  console.log('BrewFlow Load Testing - Starting');
  console.log('='.repeat(80));
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Total RPS: ${TOTAL_RPS}`);
  console.log(`  - Catalog browsing: ${CATALOG_RPS} RPS (${CATALOG_PERCENT}%)`);
  console.log(`  - Shopping: ${SHOP_RPS} RPS (${SHOP_PERCENT}%)`);
  console.log(`  - Order checking: ${CHECK_ORDERS_RPS} RPS (${CHECK_ORDERS_PERCENT}%)`);
  console.log(`  - Registration: ${REGISTER_RPS} RPS (${REGISTER_PERCENT}%)`);
  console.log(`Test Duration: ${TEST_DURATION}`);
  console.log(`Test Users: ${testUsers.length}`);
  console.log(`Products: ${PRODUCT_IDS.length}`);
  console.log('='.repeat(80));

  // Batch регистрация пользователей
  console.log('Registering test users in batches...');
  const batchSize = 10;
  let registered = 0;
  
  for (let i = 0; i < testUsers.length; i += batchSize) {
    const batch = testUsers.slice(i, i + batchSize);
    const requests = batch.map(user => ({
      method: 'POST',
      url: `${BASE_URL}/api/auth/register`,
      body: JSON.stringify({
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName
      }),
      params: { headers: { 'Content-Type': 'application/json' }, tags: { name: 'BatchRegister' } }
    }));
    
    const responses = http.batch(requests);
    registered += responses.filter(r => r.status === 200 || r.status === 201).length;
    
    // Небольшая задержка между батчами для снижения нагрузки
    sleep(0.5);
  }
  
  console.log(`Registered ${registered}/${testUsers.length} test users`);
  console.log('='.repeat(80));
}

export function teardown(data) {
  console.log('='.repeat(80));
  console.log('BrewFlow Load Testing - Completed');
  console.log('='.repeat(80));
}

