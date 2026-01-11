import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:chagok_flutter/models/moment_photo.dart';
import 'package:chagok_flutter/theme/app_theme.dart';

class MomentPhotoView extends StatelessWidget {
  const MomentPhotoView({
    super.key,
    required this.photo,
    this.borderRadius = 16,
    this.showLabel = false,
    this.fit = BoxFit.cover,
  });

  final MomentPhoto photo;
  final double borderRadius;
  final bool showLabel;
  final BoxFit fit;

  @override
  Widget build(BuildContext context) {
    final imageWidget = photo.path == null
        ? Container(
            color: photo.accent,
            child: const Center(
              child: Icon(
                Icons.photo_outlined,
                color: AppColors.main,
              ),
            ),
          )
        : Image.file(
            File(photo.path!),
            fit: fit,
            errorBuilder: (_, __, ___) => Container(
              color: photo.accent,
              child: const Center(
                child: Icon(
                  Icons.broken_image_outlined,
                  color: AppColors.main,
                ),
              ),
            ),
          );

    return ClipRRect(
      borderRadius: BorderRadius.circular(borderRadius),
      child: Stack(
        children: [
          Positioned.fill(
            child: Transform.rotate(
              angle: photo.rotationTurns * 2 * math.pi,
              child: imageWidget,
            ),
          ),
          if (showLabel)
            Positioned(
              left: 8,
              bottom: 8,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.85),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  photo.label,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
