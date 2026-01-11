import 'package:flutter/material.dart';
import 'package:chagok_flutter/screens/past_detail_screen.dart';
import 'package:chagok_flutter/state/moment_store.dart';
import 'package:chagok_flutter/theme/app_theme.dart';
import 'package:chagok_flutter/utils/date_utils.dart';
import 'package:chagok_flutter/widgets/moment_photo_view.dart';
import 'package:provider/provider.dart';

class PastScreen extends StatelessWidget {
  const PastScreen({super.key});

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
              child: Consumer<MomentStore>(
                builder: (context, store, _) {
                  final grouped = store.groupedPastMoments();
                  final entries = grouped.entries.toList();
                  if (entries.isEmpty) {
                    return Center(
                      child: Text(
                        '아직 지난 기록이 없어요.',
                        style: Theme.of(context)
                            .textTheme
                            .bodySmall
                            ?.copyWith(color: AppColors.textSecondary),
                      ),
                    );
                  }

                  return ListView.separated(
                    itemCount: entries.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final entry = entries[index];
                      final date = entry.key;
                      final moments = entry.value;
                      final representative = moments.first.mainPhoto;
                      final summaryMemo = moments.first.memo.isNotEmpty
                          ? moments.first.memo
                          : '메모 없음';
                      final countLabel = moments.length > 1
                          ? '외 ${moments.length - 1}건'
                          : '1건';

                      return InkWell(
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => PastDetailScreen(
                                date: date,
                                entries: moments,
                              ),
                            ),
                          );
                        },
                        borderRadius: BorderRadius.circular(18),
                        child: Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(18),
                            border: Border.all(color: AppColors.border),
                          ),
                          child: Row(
                            children: [
                              SizedBox(
                                width: 56,
                                height: 56,
                                child: MomentPhotoView(
                                  photo: representative,
                                  borderRadius: 16,
                                  fit: BoxFit.cover,
                                ),
                              ),
                              const SizedBox(width: 16),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      formatDate(date),
                                      style: Theme.of(context)
                                          .textTheme
                                          .bodyMedium
                                          ?.copyWith(
                                            fontWeight: FontWeight.w600,
                                          ),
                                    ),
                                    const SizedBox(height: 6),
                                    Text(
                                      summaryMemo,
                                      style: Theme.of(context)
                                          .textTheme
                                          .bodySmall
                                          ?.copyWith(
                                            color: AppColors.textSecondary,
                                          ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 12),
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(
                                    countLabel,
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
                        ),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
