import 'package:flutter/material.dart';

enum PhotoSource {
  camera,
  gallery,
}

class MomentPhoto {
  const MomentPhoto({
    required this.id,
    required this.label,
    required this.source,
    required this.accent,
    this.path,
    this.rotationTurns = 0,
  });

  final String id;
  final String label;
  final PhotoSource source;
  final Color accent;
  final String? path;
  final double rotationTurns;

  MomentPhoto copyWith({
    String? id,
    String? label,
    PhotoSource? source,
    Color? accent,
    String? path,
    double? rotationTurns,
  }) {
    return MomentPhoto(
      id: id ?? this.id,
      label: label ?? this.label,
      source: source ?? this.source,
      accent: accent ?? this.accent,
      path: path ?? this.path,
      rotationTurns: rotationTurns ?? this.rotationTurns,
    );
  }
}
