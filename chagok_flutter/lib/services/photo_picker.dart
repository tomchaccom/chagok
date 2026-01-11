import 'package:flutter/material.dart';
import 'package:chagok_flutter/models/moment_photo.dart';
import 'package:chagok_flutter/theme/app_theme.dart';

class PhotoPicker {
  PhotoPicker({DateTime? now}) : _now = now ?? DateTime.now();

  final DateTime _now;

  final List<MomentPhoto> _mockGallery = const [
    MomentPhoto(
      id: 'gallery_1',
      label: '빛이 스민 순간',
      source: PhotoSource.gallery,
      accent: Color(0xFFE3DFFD),
    ),
    MomentPhoto(
      id: 'gallery_2',
      label: '조용한 거리',
      source: PhotoSource.gallery,
      accent: Color(0xFFFDE2E4),
    ),
    MomentPhoto(
      id: 'gallery_3',
      label: '따뜻한 오후',
      source: PhotoSource.gallery,
      accent: Color(0xFFFFF1E6),
    ),
  ];

  Future<MomentPhoto?> pickFromGallery(BuildContext context) async {
    final selected = await showModalBottomSheet<MomentPhoto>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (context) {
        return Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '갤러리에서 선택',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
              ),
              const SizedBox(height: 16),
              ..._mockGallery.map(
                (photo) => ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: photo.accent,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.image_outlined, color: AppColors.main),
                  ),
                  title: Text(photo.label),
                  subtitle: const Text('선택하면 방향을 확인해요'),
                  onTap: () => Navigator.pop(context, photo),
                ),
              ),
            ],
          ),
        );
      },
    );

    if (selected == null) {
      return null;
    }

    return selected.copyWith(
      id: 'gallery_${_now.millisecondsSinceEpoch}',
    );
  }

  Future<MomentPhoto> captureFromCamera() async {
    return MomentPhoto(
      id: 'camera_${_now.millisecondsSinceEpoch}',
      label: '카메라 스냅',
      source: PhotoSource.camera,
      accent: AppColors.sub,
    );
  }
}
