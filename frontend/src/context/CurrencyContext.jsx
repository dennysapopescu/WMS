import React, { createContext, useContext, useState, useEffect } from 'react';

export const CURRENCIES = {
  RON: {
    code: 'RON',
    symbol: 'RON',
    label: 'RON (lei)',
    rateFromRon: 1.0,
    prefix: false,
  },
  EUR: {
    code: 'EUR',
    symbol: '€',
    label: 'EUR (€)',
    rateFromRon: 1 / 4.97, // 1 EUR ≈ 4.97 RON
    prefix: true,
  },
  USD: {
    code: 'USD',
    symbol: '$',
    label: 'USD ($)',
    rateFromRon: 1 / 4.60, // 1 USD ≈ 4.60 RON
    prefix: true,
  },
  GBP: {
    code: 'GBP',
    symbol: '£',
    label: 'GBP (£)',
    rateFromRon: 1 / 5.85, // 1 GBP ≈ 5.85 RON
    prefix: true,
  },
};

const CurrencyContext = createContext();

export function CurrencyProvider({ children }) {
  const [currencyCode, setCurrencyCode] = useState(() => {
    return localStorage.getItem('wms_currency') || 'RON';
  });

  const activeCurrency = CURRENCIES[currencyCode] || CURRENCIES.RON;

  useEffect(() => {
    localStorage.setItem('wms_currency', currencyCode);
  }, [currencyCode]);

  const setCurrency = (code) => {
    if (CURRENCIES[code]) {
      setCurrencyCode(code);
    }
  };

  /**
   * Formats a given monetary amount (stored with base in RON) into the selected currency.
   * @param {number|string} amount
   */
  const formatPrice = (amount) => {
    const numeric = Number(amount);
    if (isNaN(numeric)) {
      return activeCurrency.prefix ? `${activeCurrency.symbol}0.00` : `0.00 ${activeCurrency.symbol}`;
    }

    const converted = numeric * (activeCurrency.rateFromRon || 1.0);
    const formattedNum = converted.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });

    return activeCurrency.prefix
      ? `${activeCurrency.symbol}${formattedNum}`
      : `${formattedNum} ${activeCurrency.symbol}`;
  };

  /**
   * Formats only the numeric portion with conversion rate and thousands separators.
   */
  const formatValueOnly = (amount) => {
    const numeric = Number(amount);
    if (isNaN(numeric)) return '0.00';
    const converted = numeric * (activeCurrency.rateFromRon || 1.0);
    return converted.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };



  return (
    <CurrencyContext.Provider
      value={{
        currency: activeCurrency,
        currencyCode,
        setCurrency,
        formatPrice,
        formatValueOnly,
        supportedCurrencies: Object.values(CURRENCIES),
      }}
    >
      {children}
    </CurrencyContext.Provider>
  );
}

export function useCurrency() {
  const context = useContext(CurrencyContext);
  if (!context) {
    throw new Error('useCurrency must be used within a CurrencyProvider');
  }
  return context;
}
