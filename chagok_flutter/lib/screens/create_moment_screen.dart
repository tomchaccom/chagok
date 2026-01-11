import 'package:flutter/material.dart';
import 'package:chagok_flutter/screens/photo_orientation_screen.dart';
import 'package:chagok_flutter/theme/app_theme.dart';

class PhotoItem {
  const PhotoItem({
    required this.id,
    required this.label,
    required this.accent,
    required this.source,
  });

  final String id;
  final String label;
  final Color accent;
  final String source;
}

class CreateMomentScreen extends StatefulWidget {
  const CreateMomentScreen({super.key});

  static const String routeName = '/create';

  @override
  State<CreateMomentScreen> createState() => _CreateMomentScreenState();
}

class _CreateMomentScreenState extends State<CreateMomentScreen> {
  final List<PhotoItem> _selectedPhotos = [];
  final TextEditingController _memoController = TextEditingController();
  PhotoItem? _mainPhoto;
  bool _isFeatured = false;
  int _score = 5;

  final List<PhotoItem> _mockGallery = const [
    PhotoItem(
      id: 'gallery_1',
      label: '빛이 스민 순간',
      accent: Color(0xFFE3DFFD),
      source: 'gallery',
    ),
    PhotoItem(
      id: 'gallery_2',
      label: '조용한 거리',
      accent: Color(0xFFFDE2E4),
      source: 'gallery',
    ),
    PhotoItem(
      id: 'gallery_3',
      label: '따뜻한 오후',
      accent: Color(0xFFFFF1E6),
      source: 'gallery',
    ),
  ];

  @override
  void dispose() {
    _memoController.dispose();
    super.dispose();
  }

  Future<void> _openGallery() async {
    final selected = await showModalBottomSheet<PhotoItem>(
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

    if (selected == null) return;

    final confirmed = await _confirmOrientation(selected);
    if (confirmed == null) return;

    _addPhoto(confirmed);
  }

  Future<void> _openCamera() async {
    final cameraPhoto = PhotoItem(
      id: 'camera_${DateTime.now().millisecondsSinceEpoch}',
      label: '카메라 스냅',
      accent: AppColors.sub,
      source: 'camera',
    );

    final confirmed = await _confirmOrientation(cameraPhoto);
    if (confirmed == null) return;

    _addPhoto(confirmed);
  }

  Future<PhotoItem?> _confirmOrientation(PhotoItem photo) async {
    final result = await Navigator.pushNamed(
      context,
      PhotoOrientationScreen.routeName,
      arguments: PhotoOrientationArguments(photo: photo),
    );

    if (result is PhotoItem) {
      return result;
    }

    return null;
  }

  void _addPhoto(PhotoItem photo) {
    setState(() {
      _selectedPhotos.add(photo);
      _mainPhoto ??= photo;
    });
  }

  Future<void> _setAsMain(PhotoItem photo) async {
    if (_mainPhoto?.id == photo.id) {
      _showSnackBar('이미 대표 기억이 설정되어 있습니다.');
      return;
    }

    if (_mainPhoto != null) {
      final shouldReplace = await showDialog<bool>(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: const Text('대표 기억 변경'),
            content: const Text('이미 대표 기억이 설정되어 있습니다.\n현재 이미지를 대표 기억으로 변경하시겠습니까?'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('취소'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('변경'),
              ),
            ],
          );
        },
      );

      if (shouldReplace != true) {
        return;
      }
    }

    setState(() {
      _mainPhoto = photo;
    });
  }

  void _toggleFeatured(bool value) {
    setState(() {
      _isFeatured = value;
    });
  }

  void _saveMoment() {
    if (_selectedPhotos.isEmpty) {
      _showSnackBar('사진을 선택해주세요.');
      return;
    }

    // TODO: Integrate backend/API to persist the record.
    _showSnackBar('임시로 저장되었습니다.');
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text('순간 기록하기'),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildPhotoCard(),
                    const SizedBox(height: 16),
                    _buildPhotoActions(),
                    const SizedBox(height: 16),
                    _buildSelectedPhotos(),
                    const SizedBox(height: 24),
                    CheckboxListTile(
                      value: _isFeatured,
                      onChanged: (value) => _toggleFeatured(value ?? false),
                      title: const Text('대표 기억으로 설정'),
                      subtitle: const Text('오늘을 대표할 기억으로 고정됩니다.'),
                      controlAffinity: ListTileControlAffinity.leading,
                      activeColor: AppColors.main,
                    ),
                    const SizedBox(height: 16),
                    Text('이 순간을 한 줄로 남겨보세요',
                        style: Theme.of(context).textTheme.bodyMedium),
                    const SizedBox(height: 8),
                    TextField(
                      controller: _memoController,
                      maxLines: 2,
                      decoration: InputDecoration(
                        hintText: '짧은 메모를 작성해보세요…',
                        filled: true,
                        fillColor: Colors.white,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide(color: AppColors.border),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide(color: AppColors.border),
                        ),
                      ),
                    ),
                    const SizedBox(height: 24),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('오늘의 점수',
                            style: Theme.of(context).textTheme.bodyMedium),
                        Text(
                          '$_score',
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: AppColors.main,
                                fontWeight: FontWeight.w700,
                              ),
                        ),
                      ],
                    ),
                    Slider(
                      value: _score.toDouble(),
                      min: 1,
                      max: 10,
                      divisions: 9,
                      activeColor: AppColors.main,
                      onChanged: (value) {
                        setState(() {
                          _score = value.round();
                        });
                      },
                    ),
                    Text(
                      '오늘의 만족도를 숫자로 남겨요.',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(20),
              child: SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _saveMoment,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.main,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  child: const Text('이 순간 저장하기'),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPhotoCard() {
    return Container(
      height: 240,
      width: double.infinity,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.border),
      ),
      child: Stack(
        children: [
          Positioned.fill(
            child: Container(
              decoration: BoxDecoration(
                color: _mainPhoto?.accent ?? AppColors.sub,
                borderRadius: BorderRadius.circular(20),
              ),
              child: _mainPhoto == null
                  ? const Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.photo_camera, size: 40, color: AppColors.main),
                          SizedBox(height: 8),
                          Text('사진을 추가해주세요'),
                        ],
                      ),
                    )
                  : Center(
                      child: Icon(
                        Icons.photo,
                        size: 64,
                        color: AppColors.main.withOpacity(0.6),
                      ),
                    ),
            ),
          ),
          if (_mainPhoto != null)
            Positioned(
              left: 16,
              top: 16,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  '대표 기억',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildPhotoActions() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        TextButton.icon(
          onPressed: _openCamera,
          icon: const Icon(Icons.camera_alt_outlined, color: AppColors.main),
          label: const Text('카메라'),
        ),
        const SizedBox(width: 8),
        TextButton.icon(
          onPressed: _openGallery,
          icon: const Icon(Icons.photo_library_outlined, color: AppColors.main),
          label: const Text('갤러리'),
        ),
      ],
    );
  }

  Widget _buildSelectedPhotos() {
    if (_selectedPhotos.isEmpty) {
      return Text(
        '선택된 사진이 아직 없어요.',
        style: Theme.of(context).textTheme.bodySmall,
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '선택한 사진',
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
        ),
        const SizedBox(height: 12),
        SizedBox(
          height: 110,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: _selectedPhotos.length,
            separatorBuilder: (_, __) => const SizedBox(width: 12),
            itemBuilder: (context, index) {
              final photo = _selectedPhotos[index];
              final isMain = _mainPhoto?.id == photo.id;
              return Container(
                width: 140,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                    color: isMain ? AppColors.main : AppColors.border,
                    width: isMain ? 2 : 1,
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      height: 44,
                      decoration: BoxDecoration(
                        color: photo.accent,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Center(
                        child: Icon(Icons.photo, color: AppColors.main),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      photo.label,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      photo.source == 'camera' ? '카메라' : '갤러리',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const Spacer(),
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton(
                        onPressed: () => _setAsMain(photo),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: AppColors.main,
                          side: BorderSide(color: AppColors.main),
                          padding: const EdgeInsets.symmetric(vertical: 4),
                        ),
                        child: Text(isMain ? '대표 설정됨' : '대표로 지정'),
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}
