import 'package:flutter/material.dart';
import 'package:chagok_flutter/models/moment_entry.dart';
import 'package:chagok_flutter/theme/app_theme.dart';
import 'package:chagok_flutter/utils/date_utils.dart';
import 'package:chagok_flutter/widgets/moment_photo_view.dart';

class PastDetailScreen extends StatelessWidget {
  const PastDetailScreen({
    super.key,
    required this.date,
    required this.entries,
  });

  final DateTime date;
  final List<MomentEntry> entries;

  @override
  Widget build(BuildContext context) {
    final label = formatDate(date);
    return Scaffold(
      appBar: AppBar(
        title: Text(label),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: SafeArea(
        child: ListView.separated(
          padding: const EdgeInsets.all(20),
          itemCount: entries.length,
          separatorBuilder: (_, __) => const SizedBox(height: 16),
          itemBuilder: (context, index) {
            final entry = entries[index];
            return Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: AppColors.border),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      SizedBox(
                        width: 72,
                        height: 72,
                        child: MomentPhotoView(
                          photo: entry.mainPhoto,
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
                              entry.memo.isNotEmpty ? entry.memo : '메모 없음',
                              style: Theme.of(context)
                                  .textTheme
                                  .bodyMedium
                                  ?.copyWith(fontWeight: FontWeight.w600),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              '점수 ${entry.score}점',
                              style: Theme.of(context)
                                  .textTheme
                                  .bodySmall
                                  ?.copyWith(color: AppColors.textSecondary),
                            ),
                          ],
                        ),
                      ),
                      if (entry.isFeatured)
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 6,
                          ),
                          decoration: BoxDecoration(
                            color: AppColors.sub,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            '대표',
                            style: Theme.of(context)
                                .textTheme
                                .bodySmall
                                ?.copyWith(color: AppColors.main),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    height: 72,
                    child: ListView.separated(
                      scrollDirection: Axis.horizontal,
                      itemCount: entry.photos.length,
                      separatorBuilder: (_, __) => const SizedBox(width: 8),
                      itemBuilder: (context, photoIndex) {
                        final photo = entry.photos[photoIndex];
                        return SizedBox(
                          width: 72,
                          height: 72,
                          child: MomentPhotoView(
                            photo: photo,
                            borderRadius: 12,
                            fit: BoxFit.cover,
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}
