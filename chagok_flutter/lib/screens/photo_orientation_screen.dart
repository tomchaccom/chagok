import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:chagok_flutter/models/moment_photo.dart';
import 'package:chagok_flutter/theme/app_theme.dart';

class PhotoOrientationArguments {
  const PhotoOrientationArguments({required this.photo});

  final MomentPhoto photo;
}

class PhotoOrientationScreen extends StatefulWidget {
  const PhotoOrientationScreen({super.key, required this.arguments});

  static const String routeName = '/orientation';

  final PhotoOrientationArguments arguments;

  @override
  State<PhotoOrientationScreen> createState() => _PhotoOrientationScreenState();
}

class _PhotoOrientationScreenState extends State<PhotoOrientationScreen> {
  late double _rotationTurns;

  @override
  void initState() {
    super.initState();
    _rotationTurns = widget.arguments.photo.rotationTurns;
  }

  void _rotate() {
    setState(() {
      _rotationTurns = (_rotationTurns + 0.25) % 1;
    });
  }

  @override
  Widget build(BuildContext context) {
    final photo = widget.arguments.photo;
    final rotatedPhoto = photo.copyWith(rotationTurns: _rotationTurns);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '사진 방향을 확인해주세요',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 8),
              Text(
                '원하는 방향으로 맞춘 뒤 저장해요.',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 24),
              Expanded(
                child: Container(
                  width: double.infinity,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(color: AppColors.border),
                  ),
                  child: Center(
                    child: Transform.rotate(
                      angle: _rotationTurns * 2 * math.pi,
                      child: Container(
                        width: 180,
                        height: 180,
                        decoration: BoxDecoration(
                          color: photo.accent,
                          borderRadius: BorderRadius.circular(24),
                        ),
                        child: photo.path == null
                            ? const Icon(
                                Icons.photo,
                                size: 72,
                                color: AppColors.main,
                              )
                            : ClipRRect(
                                borderRadius: BorderRadius.circular(24),
                                child: Image.file(
                                  File(photo.path!),
                                  fit: BoxFit.cover,
                                  errorBuilder: (_, __, ___) => const Icon(
                                    Icons.broken_image_outlined,
                                    size: 72,
                                    color: AppColors.main,
                                  ),
                                ),
                              ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: _rotate,
                  icon: const Icon(Icons.rotate_right),
                  label: const Text('90° 회전'),
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('다시 선택'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => Navigator.pop(context, rotatedPhoto),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.main,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                        ),
                      ),
                      child: const Text('이 방향으로 저장'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
