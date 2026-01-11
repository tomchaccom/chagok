import 'package:flutter/material.dart';
import 'package:chagok_flutter/theme/app_theme.dart';

class FutureMemory {
  const FutureMemory({
    required this.id,
    required this.title,
    required this.date,
    required this.detail,
  });

  final String id;
  final String title;
  final String date;
  final String detail;
}

class FutureScreen extends StatelessWidget {
  const FutureScreen({super.key});

  static const List<FutureMemory> _mockPlans = [
    FutureMemory(
      id: 'future_1',
      title: '바닷가 일몰 기록',
      date: '2024.09.12',
      detail: '파도 소리를 담아두기',
    ),
    FutureMemory(
      id: 'future_2',
      title: '친구와 사진 산책',
      date: '2024.10.04',
      detail: '서로의 순간을 기록하기',
    ),
    FutureMemory(
      id: 'future_3',
      title: '전시회 방문',
      date: '2024.10.18',
      detail: '작품 앞에서 느낀 감정 적기',
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
              '미래의 계획',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 8),
            Text(
              '다가올 기억을 차곡차곡 모아봐요.',
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(color: AppColors.textSecondary),
            ),
            const SizedBox(height: 24),
            Expanded(
              child: ListView.separated(
                itemCount: _mockPlans.length,
                separatorBuilder: (_, __) => const SizedBox(height: 12),
                itemBuilder: (context, index) {
                  final plan = _mockPlans[index];
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
                            Icons.event_available_outlined,
                            color: AppColors.main,
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                plan.title,
                                style: Theme.of(context)
                                    .textTheme
                                    .bodyMedium
                                    ?.copyWith(fontWeight: FontWeight.w600),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                plan.detail,
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
                              plan.date,
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
              'TODO: 미래 계획은 백엔드/로컬 저장소 연동 후 불러옵니다.',
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
