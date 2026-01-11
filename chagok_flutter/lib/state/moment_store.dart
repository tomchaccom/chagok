import 'package:flutter/material.dart';
import 'package:chagok_flutter/models/future_plan.dart';
import 'package:chagok_flutter/models/moment_entry.dart';
import 'package:chagok_flutter/models/moment_photo.dart';
import 'package:chagok_flutter/utils/date_utils.dart';

class MomentStore extends ChangeNotifier {
  MomentStore() {
    _seedData();
  }

  final List<MomentEntry> _moments = [];
  final List<FuturePlan> _futurePlans = [];

  List<MomentEntry> get moments => List.unmodifiable(_moments);
  List<FuturePlan> get futurePlans => List.unmodifiable(_futurePlans);

  List<MomentEntry> get presentMoments {
    final today = dateOnly(DateTime.now());
    return _moments
        .where((moment) => dateOnly(moment.date) == today)
        .toList(growable: false);
  }

  List<MomentEntry> get pastMoments {
    final today = dateOnly(DateTime.now());
    return _moments
        .where((moment) => dateOnly(moment.date).isBefore(today))
        .toList(growable: false);
  }

  Map<DateTime, List<MomentEntry>> groupedPastMoments() {
    final Map<DateTime, List<MomentEntry>> grouped = {};
    for (final moment in pastMoments) {
      final key = dateOnly(moment.date);
      grouped.putIfAbsent(key, () => []).add(moment);
    }
    for (final entry in grouped.entries) {
      entry.value.sort((a, b) => b.date.compareTo(a.date));
    }
    final sortedKeys = grouped.keys.toList()
      ..sort((a, b) => b.compareTo(a));
    return {for (final key in sortedKeys) key: grouped[key]!};
  }

  void addMoment(MomentEntry entry) {
    _moments.insert(0, entry);
    notifyListeners();
  }

  void _seedData() {
    final now = DateTime.now();
    _moments.addAll([
      MomentEntry(
        id: 'seed_1',
        date: now.subtract(const Duration(days: 1)),
        memo: '햇살이 유난히 부드러웠던 날',
        score: 8,
        photos: const [
          MomentPhoto(
            id: 'seed_1_photo',
            label: '따뜻한 오후',
            source: PhotoSource.gallery,
            accent: Color(0xFFFFF1E6),
          ),
        ],
        mainPhotoId: 'seed_1_photo',
        isFeatured: true,
      ),
      MomentEntry(
        id: 'seed_2',
        date: now.subtract(const Duration(days: 2)),
        memo: '작은 메모와 커피 향',
        score: 7,
        photos: const [
          MomentPhoto(
            id: 'seed_2_photo',
            label: '카페 창가',
            source: PhotoSource.gallery,
            accent: Color(0xFFFDE2E4),
          ),
        ],
        mainPhotoId: 'seed_2_photo',
        isFeatured: false,
      ),
      MomentEntry(
        id: 'seed_3',
        date: now.subtract(const Duration(days: 2)),
        memo: '우산 아래서 웃던 순간',
        score: 9,
        photos: const [
          MomentPhoto(
            id: 'seed_3_photo',
            label: '비 내린 저녁',
            source: PhotoSource.gallery,
            accent: Color(0xFFE3DFFD),
          ),
        ],
        mainPhotoId: 'seed_3_photo',
        isFeatured: false,
      ),
    ]);

    _futurePlans.addAll([
      FuturePlan(
        id: 'future_1',
        title: '바닷가 일몰 기록',
        date: dateOnly(now.add(const Duration(days: 7))),
        detail: '파도 소리를 담아두기',
      ),
      FuturePlan(
        id: 'future_2',
        title: '친구와 사진 산책',
        date: dateOnly(now.add(const Duration(days: 14))),
        detail: '서로의 순간을 기록하기',
      ),
      FuturePlan(
        id: 'future_3',
        title: '전시회 방문',
        date: dateOnly(now.add(const Duration(days: 21))),
        detail: '작품 앞에서 느낀 감정 적기',
      ),
    ]);
    _futurePlans.sort((a, b) => a.date.compareTo(b.date));
  }
}
