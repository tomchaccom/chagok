import 'package:chagok_flutter/models/moment_photo.dart';

class MomentEntry {
  const MomentEntry({
    required this.id,
    required this.date,
    required this.memo,
    required this.score,
    required this.photos,
    required this.mainPhotoId,
    required this.isFeatured,
  });

  final String id;
  final DateTime date;
  final String memo;
  final int score;
  final List<MomentPhoto> photos;
  final String mainPhotoId;
  final bool isFeatured;

  MomentPhoto get mainPhoto {
    return photos.firstWhere(
      (photo) => photo.id == mainPhotoId,
      orElse: () => photos.first,
    );
  }
}
