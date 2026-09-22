import { describe, it, expect } from 'vitest';
import { encryptIdCard, decryptIdCard } from '@/utils/secure';

describe('Utils:secure - 身份证号加解密', () => {
  describe('encryptIdCard', () => {
    it('空字符串应返回空字符串', () => {
      expect(encryptIdCard('')).toBe('');
    });

    it('null/undefined 应返回空字符串', () => {
      expect(encryptIdCard(null)).toBe('');
      expect(encryptIdCard(undefined)).toBe('');
      expect(encryptIdCard(0)).toBe('');
      expect(encryptIdCard(false)).toBe('');
    });

    it('普通身份证号应返回 base64 字符串', () => {
      const result = encryptIdCard('110101199003077734');
      expect(typeof result).toBe('string');
      expect(result.length).toBeGreaterThan(0);
      // 应为合法 base64 字符
      expect(/^[A-Za-z0-9+/=]+$/.test(result)).toBe(true);
    });

    it('相同输入应产生相同输出', () => {
      const id = '110101199003077734';
      expect(encryptIdCard(id)).toBe(encryptIdCard(id));
    });

    it('加密后应与原值不同（反转+base64）', () => {
      const id = '110101199003077734';
      expect(encryptIdCard(id)).not.toBe(id);
    });
  });

  describe('decryptIdCard', () => {
    it('空字符串应返回空字符串', () => {
      expect(decryptIdCard('')).toBe('');
    });

    it('null/undefined 应返回空字符串', () => {
      expect(decryptIdCard(null)).toBe('');
      expect(decryptIdCard(undefined)).toBe('');
    });

    it('应解密为原始身份证号', () => {
      const original = '110101199003077734';
      const encrypted = encryptIdCard(original);
      expect(decryptIdCard(encrypted)).toBe(original);
    });

    it('应解密为另一身份证号', () => {
      const original = '440101198001012345';
      const encrypted = encryptIdCard(original);
      expect(decryptIdCard(encrypted)).toBe(original);
    });
  });

  describe('加解密可逆性', () => {
    it('decrypt(encrypt(x)) === x 对任意字符串成立', () => {
      const samples = [
        '110101199003077734',
        '440101198001012345',
        '123456789012345678',
        '1',
        'a',
        'X',
        'ABCDEF123456',
      ];
      for (const sample of samples) {
        const encrypted = encryptIdCard(sample);
        const decrypted = decryptIdCard(encrypted);
        expect(decrypted).toBe(sample);
      }
    });

    it('数字字符串加解密应保持一致', () => {
      const num = '12345';
      expect(decryptIdCard(encryptIdCard(num))).toBe(num);
    });

    it('字母字符串加解密应保持一致', () => {
      const str = 'ABCDefgh';
      expect(decryptIdCard(encryptIdCard(str))).toBe(str);
    });

    it('单个字符加解密应保持一致', () => {
      const single = 'X';
      expect(decryptIdCard(encryptIdCard(single))).toBe(single);
    });
  });

  describe('加密逻辑验证', () => {
    it('加密过程应为：反转字符串后 base64 编码', () => {
      const id = '12345';
      // 手动计算预期结果
      const reversed = '54321';
      const buffer = Buffer.from(new TextEncoder().encode(reversed));
      const expected = buffer.toString('base64');
      expect(encryptIdCard(id)).toBe(expected);
    });

    it('不同输入应产生不同输出', () => {
      const a = encryptIdCard('110101199003077734');
      const b = encryptIdCard('440101198001012345');
      expect(a).not.toBe(b);
    });
  });
});
