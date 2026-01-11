import 'package:flutter/material.dart';
import 'package:chagok_flutter/theme/app_theme.dart';

class PastMemory {
  const PastMemory({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.emotion,
  });

  final String id;
  final String title;
  final String subtitle;
  final String emotion;
}

class PastScreen extends StatelessWidget {
  const PastScreen({super.key});

  static const List<PastMemory> _mockMemories = [
    PastMemory(
      id: 'past_1',
      title: '따뜻한 오후의 산책',
      subtitle: '햇살이 유난히 부드러웠던 날',
      emotion: '잔잔함',
    ),
    PastMemory(
      id: 'past_2',
      title: '카페 창가 자리',
      subtitle: '작은 메모와 커피 향',
      emotion: '안정감',
    ),
    PastMemory(
      id: 'past_3',
      title: '비가 내린 저녁',
      subtitle: '우산 아래서 웃던 순간',
      emotion: '설렘',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '과거의 기록',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 8),
            Text(
              '지나간 순간을 조용히 다시 바라봐요.',
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(color: AppColors.textSecondary),
            ),
            const SizedBox(height: 24),
            Expanded(
              child: ListView.separated(
                itemCount: _mockMemories.length,
                separatorBuilder: (_, __) => const SizedBox(height: 12),
                itemBuilder: (context, index) {
                  final memory = _mockMemories[index];
                  return Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 56,
                          height: 56,
                          decoration: BoxDecoration(
                            color: AppColors.sub,
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: const Icon(
                            Icons.photo_outlined,
                            color: AppColors.main,
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                memory.title,
                                style: Theme.of(context)
                                    .textTheme
                                    .bodyMedium
                                    ?.copyWith(fontWeight: FontWeight.w600),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                memory.subtitle,
                                style: Theme.of(context)
                                    .textTheme
                                    .bodySmall
                                    ?.copyWith(color: AppColors.textSecondary),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 12),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              memory.emotion,
                              style: Theme.of(context)
                                  .textTheme
                                  .bodySmall
                                  ?.copyWith(color: AppColors.main),
                            ),
                            const SizedBox(height: 8),
                            const Icon(
                              Icons.chevron_right,
                              color: AppColors.textSecondary,
                            ),
                          ],
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'TODO: 과거 기록은 백엔드/로컬 저장소 연동 후 불러옵니다.',
              style: Theme.of(context)
                  .textTheme
                  .bodySmall
                  ?.copyWith(color: AppColors.textSecondary),
            ),
          ],
        ),
      ),
    );
  }
}
