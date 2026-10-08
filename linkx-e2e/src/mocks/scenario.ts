export type TestScenario = {
  user: {
    userid: string;
    username: string;
    aastoken: string;
    idCard?: string;
  };
  storage: Record<string, string>;
  groups: Array<{ id: string; name: string }>;
  tasks: Array<{ id: string; title: string; status: string }>;
};

export function createDefaultScenario(): TestScenario {
  return {
    user: {
      userid: 'test-user-001',
      username: '自动化测试用户',
      aastoken: 'test-aas-token',
      idCard: '130000199001011234',
    },
    storage: {},
    groups: [],
    tasks: [],
  };
}
