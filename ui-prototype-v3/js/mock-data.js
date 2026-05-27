/**
 * IRAgent v3 — Mock Data
 * 模拟数据：用户、笔记、考点、试卷、错题、对话
 */
const MOCK = {

  user: {
    name: '小明',
    avatar: '小',
    email: 'student_demo@iragent.ai',
    examType: '考研',
    subjects: ['数学', '英语', '政治'],
    targetScore: { math: 130, english: 70, politics: 75 },
    joinedAt: '2026-03-15'
  },

  notes: [
    {
      id: 'n1', subject: '数学', chapter: '导数与微分',
      title: '导数公式与求导法则',
      content: '基本初等函数导数公式：\n(C)\'=0\n(x^n)\'=n·x^(n-1)\n(sin x)\'=cos x\n(cos x)\'=-sin x\n(e^x)\'=e^x\n(ln x)\'=1/x\n\n四则运算法则：\n(u±v)\'=u\'±v\'\n(uv)\'=u\'v+uv\'\n(u/v)\'=(u\'v-uv\')/v²',
      tags: ['求导公式', '四则运算', '基本函数'],
      createdAt: '2026-05-10',
      linkedQuestions: ['q1', 'q2']
    },
    {
      id: 'n2', subject: '数学', chapter: '导数应用',
      title: '闭区间最值 —— 三步法',
      content: '求 f(x) 在 [a,b] 上最值：\n1. 求导 f\'(x)，解 f\'(x)=0 找驻点\n2. 计算 f(驻点), f(a), f(b)\n3. 排序比较，得出最大值和最小值\n\n易错点：忘记计算端点值！',
      tags: ['最值问题', '驻点', '端点比较'],
      createdAt: '2026-05-12',
      linkedQuestions: ['q1']
    },
    {
      id: 'n3', subject: '数学', chapter: '三角函数',
      title: '三角恒等变换公式',
      content: '和差角公式：\nsin(α±β)=sinαcosβ±cosαsinβ\ncos(α±β)=cosαcosβ∓sinαsinβ\n\n二倍角：\nsin2α=2sinαcosα\ncos2α=cos²α-sin²α=2cos²α-1=1-2sin²α',
      tags: ['和差角', '二倍角', '诱导公式'],
      createdAt: '2026-05-08',
      linkedQuestions: ['q3']
    },
    {
      id: 'n4', subject: '英语', chapter: '语法-虚拟语气',
      title: '虚拟语气用法总结',
      content: '与现在事实相反：If I were you, I would...\n与过去事实相反：If I had known, I would have...\n与将来事实相反：If it were to rain, I would...\n\n注意：were 用于所有人称',
      tags: ['if条件句', '时态倒退', 'were'],
      createdAt: '2026-05-05',
      linkedQuestions: []
    },
    {
      id: 'n5', subject: '政治', chapter: '马原-唯物辩证法',
      title: '对立统一规律',
      content: '矛盾同一性与斗争性的辩证关系：\n1. 同一性：矛盾双方相互依存、相互贯通\n2. 斗争性：矛盾双方相互排斥、相互对立\n3. 关系：同一性是有条件的、相对的；斗争性是无条件的、绝对的\n\n矛盾分析法是认识事物的根本方法。',
      tags: ['矛盾', '对立统一', '唯物辩证法'],
      createdAt: '2026-05-15',
      linkedQuestions: []
    }
  ],

  knowledgePoints: [
    { id: 'kp1', name: '导数的定义', chapter: '导数与微分', mastery: 88, linkedNotes: ['n1'], weight: 'high' },
    { id: 'kp2', name: '求导公式', chapter: '导数与微分', mastery: 92, linkedNotes: ['n1'], weight: 'high' },
    { id: 'kp3', name: '闭区间最值', chapter: '导数应用', mastery: 65, linkedNotes: ['n2'], weight: 'high' },
    { id: 'kp4', name: '单调性与极值', chapter: '导数应用', mastery: 78, linkedNotes: ['n2'], weight: 'high' },
    { id: 'kp5', name: '三角恒等变换', chapter: '三角函数', mastery: 70, linkedNotes: ['n3'], weight: 'medium' },
    { id: 'kp6', name: '虚拟语气', chapter: '语法', mastery: 85, linkedNotes: ['n4'], weight: 'medium' },
    { id: 'kp7', name: '对立统一规律', chapter: '马原', mastery: 80, linkedNotes: ['n5'], weight: 'high' },
    { id: 'kp8', name: '矛盾分析法', chapter: '马原', mastery: 72, linkedNotes: ['n5'], weight: 'high' }
  ],

  examPapers: [
    {
      id: 'p1', title: '2024 考研数学一真题',
      totalScore: 150, duration: 120,
      questions: [
        { id: 'pq1', type: '选择', number: 1, points: 5, topic: '集合与逻辑', correctAnswer: 'C', userAnswer: 'B', isCorrect: false },
        { id: 'pq2', type: '选择', number: 2, points: 5, topic: '复数', correctAnswer: 'B', userAnswer: 'B', isCorrect: true },
        { id: 'pq3', type: '选择', number: 3, points: 5, topic: '向量', correctAnswer: 'A', userAnswer: 'A', isCorrect: true },
        { id: 'pq4', type: '选择', number: 4, points: 5, topic: '三角函数', correctAnswer: 'C', userAnswer: 'A', isCorrect: false },
        { id: 'pq5', type: '选择', number: 5, points: 5, topic: '函数性质', correctAnswer: 'B', userAnswer: 'B', isCorrect: true },
        { id: 'pq6', type: '填空', number: 13, points: 5, topic: '导数运算', correctAnswer: '6', userAnswer: '3', isCorrect: false },
        { id: 'pq7', type: '填空', number: 14, points: 5, topic: '解析几何', correctAnswer: '2√3', userAnswer: '√3', isCorrect: false },
        { id: 'pq8', type: '解答', number: 17, points: 12, topic: '导数应用', correctAnswer: '最大值=4, 最小值=0', userAnswer: '最大值=4, 最小值=0', isCorrect: true },
        { id: 'pq9', type: '解答', number: 18, points: 12, topic: '概率统计', correctAnswer: '0.65', userAnswer: '0.65', isCorrect: true },
        { id: 'pq10', type: '解答', number: 19, points: 12, topic: '数列', correctAnswer: 'an=2n-1', userAnswer: 'an=2n-1²', isCorrect: false }
      ]
    }
  ],

  errorQuestions: [
    {
      id: 'eq1',
      source: '2024考研数学一真题 第6题',
      question: '求函数 f(x) = x³ - 6x² + 9x 在区间 [0, 4] 上的最大值和最小值',
      userAnswer: '最大值 = 4，最小值 = 0',
      correctAnswer: '最大值 = 4（x=1或4），最小值 = 0（x=0或3）',
      isCorrectByChance: true,
      diagnosis: {
        knowledge: {
          title: '考点漏缺',
          icon: '📚',
          analysis: '题目考察闭区间最值问题。你的方法遗漏了端点值比较这一关键步骤。正确做法是同时计算驻点值和端点值，然后排序比较。你只算了驻点，巧合地得到了正确答案，但方法存在系统性缺陷。',
          missing: ['端点值比较', '候选值排序'],
          linkedNotes: ['n2']
        },
        formula: {
          title: '公式混淆',
          icon: '📐',
          analysis: '你把"驻点"（f\'(x)=0 的解）等同于"最值点"。在闭区间上，极值点 ≠ 最值点。最值必须在驻点和端点中比较得出。',
          confusion: ['驻点 vs 极值点 vs 最值点'],
          linkedNotes: ['n1', 'n2']
        },
        calculation: {
          title: '计算失误',
          icon: '🔢',
          analysis: '你的计算本身正确（f(0)=0, f(1)=4, f(3)=0），但漏算了 f(4)=4。在区间 [0,5] 上这个错误就会暴露。',
          errorDetail: '漏算 x=4 处函数值',
          linkedNotes: ['n2']
        }
      },
      similarQuestions: [
        { id: 'sq1', title: '求 f(x)=x³-3x² 在 [-1,3] 上的最值', difficulty: 'medium' },
        { id: 'sq2', title: '求 f(x)=sin x + cos x 在 [0,π] 上的最值', difficulty: 'medium' },
        { id: 'sq3', title: '求 f(x)=x⁴-2x²+3 在 [-2,2] 上的最值', difficulty: 'hard' }
      ],
      reviewSchedule: { nextReview: '2026-05-24', interval: 3, easeFactor: 2.5 }
    },
    {
      id: 'eq2',
      source: '2024考研数学一真题 第14题',
      question: '若双曲线 C: x²/a² - y²/b² = 1 的一条渐近线方程为 y = 2x，则 C 的离心率为_____',
      userAnswer: '√3',
      correctAnswer: '2√3',
      diagnosis: {
        knowledge: {
          title: '考点漏缺',
          icon: '📚',
          analysis: '考察双曲线渐近线与离心率的关系。你掌握了渐近线斜率公式 b/a=2，但离心率化简时出错。',
          missing: ['离心率公式变形'],
          linkedNotes: []
        },
        formula: {
          title: '公式混淆',
          icon: '📐',
          analysis: '离心率 e=c/a，其中 c²=a²+b²。b/a=2 → b=2a → c²=a²+4a²=5a² → e=√5。你可能把 e 和 √(c/a) 搞混了。',
          confusion: ['e=c/a', 'c²=a²+b² 代入'],
          linkedNotes: []
        },
        calculation: {
          title: '计算失误',
          icon: '🔢',
          analysis: 'b/a=2 → b²=4a² → c²=5a² → e=√5≈2.236。正确答案应为 √5（不是 2√3）。等等——让我检查你的笔记。',
          errorDetail: '代入公式时混淆 c²=a²+b² 的计算',
          linkedNotes: []
        }
      },
      similarQuestions: [
        { id: 'sq4', title: '椭圆离心率计算（给长轴和焦距）', difficulty: 'easy' },
        { id: 'sq5', title: '双曲线渐近线与离心率综合', difficulty: 'medium' }
      ],
      reviewSchedule: { nextReview: '2026-05-23', interval: 1, easeFactor: 2.5 }
    }
  ],

  chatHistory: [
    {
      id: 'c1',
      title: '二次函数求极值方法',
      lastMessage: '你已经学会了配方法和求导法...',
      time: '2026-05-21 14:30',
      unread: 5
    },
    {
      id: 'c2',
      title: '英语虚拟语气练习',
      lastMessage: 'If I were you, I would review this again.',
      time: '2026-05-20 16:00',
      unread: 0
    }
  ],

  todayTasks: [
    { type: 'review', title: '复习 3 道错题', detail: '间隔复习队列中', progress: 0 },
    { type: 'practice', title: '每日一练 · 5 题', detail: '限时 15 分钟', progress: 0 },
    { type: 'weakness', title: '专项突破 · 闭区间最值', detail: '掌握度仅 65%', progress: 0 }
  ],

  weeklyReport: {
    week: '2026-05-15 ~ 2026-05-21',
    studyTime: '8.5h',
    questionsDone: 67,
    accuracy: '72%',
    mastered: 3,
    improved: 5
  }
};

// Prevent accidental mutation
Object.freeze(MOCK.user);
Object.freeze(MOCK.weeklyReport);
