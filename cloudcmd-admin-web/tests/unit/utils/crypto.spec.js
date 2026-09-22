import { describe, it, expect, vi, beforeEach } from 'vitest';
import CryptoJS from 'crypto-js';
import { encryptJson, decryptJson } from '@/utils/crypto';

describe('Utils:crypto - AES 加解密', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  describe('encryptJson', () => {
    it('应成功加密对象并返回字符串', () => {
      const result = encryptJson({ a: 1 });
      expect(typeof result).toBe('string');
      expect(result.length).toBeGreaterThan(0);
    });

    it('加密结果不应包含原始明文', () => {
      const result = encryptJson({ name: 'kooriookami' });
      expect(result).not.toContain('kooriookami');
    });

    it('相同对象加密两次解密后应得到相同原文（同密钥）', () => {
      // 注：CryptoJS AES passphrase 模式会随机生成 salt，密文每次不同
      const obj = { name: 'test', age: 25 };
      const decrypted1 = decryptJson(encryptJson(obj));
      const decrypted2 = decryptJson(encryptJson(obj));
      expect(decrypted1).toEqual(obj);
      expect(decrypted2).toEqual(obj);
    });

    it('嵌套对象应能正常加密', () => {
      const obj = { user: { name: 'test', roles: ['admin', 'user'] } };
      const result = encryptJson(obj);
      expect(typeof result).toBe('string');
      expect(result.length).toBeGreaterThan(0);
    });

    it('数组应能正常加密', () => {
      const arr = [1, 2, 3, 'four', { five: 5 }];
      const result = encryptJson(arr);
      expect(typeof result).toBe('string');
      expect(result.length).toBeGreaterThan(0);
    });

    it('空对象应能正常加密', () => {
      const result = encryptJson({});
      expect(typeof result).toBe('string');
      expect(result.length).toBeGreaterThan(0);
    });

    it('加密失败时应抛出错误', () => {
      // 传入无法被 JSON.stringify 的值（循环引用）
      const circular = {};
      circular.self = circular;
      expect(() => encryptJson(circular)).toThrow();
    });
  });

  describe('decryptJson', () => {
    it('应成功解密为原始对象', () => {
      const original = { name: 'kooriookami', age: 25 };
      const encrypted = encryptJson(original);
      const decrypted = decryptJson(encrypted);
      expect(decrypted).toEqual(original);
    });

    it('应 deep equal 原始对象', () => {
      const original = { a: 1 };
      const encrypted = encryptJson(original);
      const decrypted = decryptJson(encrypted);
      expect(decrypted).toEqual({ a: 1 });
    });

    it('嵌套对象解密应保持结构', () => {
      const original = { user: { name: 'test', roles: ['admin', 'user'] } };
      const encrypted = encryptJson(original);
      const decrypted = decryptJson(encrypted);
      expect(decrypted).toEqual(original);
    });

    it('数组解密应保持结构', () => {
      const original = [1, 2, 3, 'four', { five: 5 }];
      const encrypted = encryptJson(original);
      const decrypted = decryptJson(encrypted);
      expect(decrypted).toEqual(original);
    });

    it('空对象解密应返回空对象', () => {
      const encrypted = encryptJson({});
      const decrypted = decryptJson(encrypted);
      expect(decrypted).toEqual({});
    });

    it('空字符串解密应返回 null', () => {
      expect(decryptJson('')).toBeNull();
    });

    it('错误密钥（损坏数据）解密应返回 null', () => {
      expect(decryptJson('invalid-encrypted-string')).toBeNull();
    });

    it('完全乱码字符串解密应返回 null', () => {
      expect(decryptJson('!@#$%^&*()')).toBeNull();
    });

    it('解密失败时应记录错误日志', () => {
      decryptJson('invalid-data');
      expect(console.error).toHaveBeenCalled();
    });

    it('解密结果非合法 JSON 字符串时应走 catch 分支并返回 null', () => {
      // 用相同密钥加密一个非 JSON 字符串，解密后得到 'hello'，JSON.parse 会抛错
      const SECRET_KEY = '8Jq4TvRz1WnXcY7bP9oK2mL6QvAsDfGhN1jUx3Zc5B6Y8tW0eRq==';
      const encrypted = CryptoJS.AES.encrypt('hello', SECRET_KEY).toString();
      expect(decryptJson(encrypted)).toBeNull();
      expect(console.error).toHaveBeenCalledWith(
        '❌ 解密失败：',
        expect.any(SyntaxError),
      );
    });

    it('解密结果为 JSON 数组字符串应能正常解析', () => {
      // 验证 catch 分支不会被错误触发：合法 JSON 数组应正常返回
      const SECRET_KEY = '8Jq4TvRz1WnXcY7bP9oK2mL6QvAsDfGhN1jUx3Zc5B6Y8tW0eRq==';
      const encrypted = CryptoJS.AES.encrypt('[1,2,3]', SECRET_KEY).toString();
      expect(decryptJson(encrypted)).toEqual([1, 2, 3]);
    });
  });

  describe('加解密可逆性', () => {
    it('decrypt(encrypt(obj)) 应 deep equal obj', () => {
      const samples = [
        { a: 1 },
        { name: 'test', age: 25 },
        { nested: { deep: { value: 42 } } },
        { list: [1, 2, 3], map: { a: 'b' } },
        { bool: true, nil: null, num: 3.14 },
      ];
      for (const sample of samples) {
        const encrypted = encryptJson(sample);
        const decrypted = decryptJson(encrypted);
        expect(decrypted).toEqual(sample);
      }
    });

    it('解密后应是新的对象引用（非同一引用）', () => {
      const original = { a: 1 };
      const encrypted = encryptJson(original);
      const decrypted = decryptJson(encrypted);
      expect(decrypted).not.toBe(original);
      expect(decrypted).toEqual(original);
    });
  });
});
