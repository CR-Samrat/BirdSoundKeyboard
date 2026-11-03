import React, { useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  NativeModules,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

const birdSounds = [
  { id: '1', name: 'Sparrow', emoji: '🐦', description: 'Cheerful chirping' },
  { id: '2', name: 'Robin', emoji: '🐤', description: 'Sweet bird song' },
  { id: '3', name: 'Nightingale', emoji: '🎶', description: 'Melodious calls' },
  { id: '4', name: 'Parrot', emoji: '🦜', description: 'Playful sounds' },
];

export default function App() {
  const [selectedBird, setSelectedBird] = useState('1');
  const [isLoaded, setIsLoaded] = useState(false);
  const [setupMessage, setSetupMessage] = useState(
    'Enable BirdSound Keyboard in Android settings, then select it as your active keyboard.',
  );

  useEffect(() => {
    const loadSelectedBird = async () => {
      try {
        const savedBird = await AsyncStorage.getItem('selectedBird');

        const birdId = birdSounds.some((bird) => bird.id === savedBird)
          ? savedBird!
          : '1';

        setSelectedBird(birdId);
        await NativeModules.BirdSound.setSelectedBird(birdId);
      } catch (error) {
        console.error('Failed to restore bird preference:', error);
      } finally {
        setIsLoaded(true);
      }
    };

    loadSelectedBird();
  }, []);

  const selectBird = async (birdId: string) => {
    if (!birdSounds.some((bird) => bird.id === birdId)) return;

    setSelectedBird(birdId);

    try {
      await AsyncStorage.setItem('selectedBird', birdId);
      await NativeModules.BirdSound.setSelectedBird(birdId);
    } catch (error: any) {
      setSetupMessage(
        `Could not save your bird selection: ${error?.message ?? String(error)}`,
      );
    }
  };

  const openKeyboardSettings = async () => {
    try {
      await NativeModules.BirdSound.openInputMethodSettings();
    } catch (error: any) {
      setSetupMessage(
        `Could not open Android keyboard settings: ${error?.message ?? String(error)}`,
      );
    }
  };

  if (!isLoaded) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" />
        <Text style={styles.tagline}>Loading your bird preference...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" />

      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <Text style={styles.logo}>🐦</Text>
          <View>
            <Text style={styles.title}>BirdSound</Text>
            <Text style={styles.subtitle}>KEYBOARD</Text>
          </View>
        </View>

        <Text style={styles.tagline}>Let your keyboard sing.</Text>

        <View style={styles.setupCard}>
          <Text style={styles.sectionTitle}>Your keyboard</Text>
          <Text style={styles.cardDescription}>
            Bring the sounds of nature to every keystroke.
          </Text>

          <TouchableOpacity
            style={styles.setupButton}
            onPress={openKeyboardSettings}
            activeOpacity={0.8}
          >
            <Text style={styles.setupButtonText}>Set up keyboard →</Text>
          </TouchableOpacity>

          <Text style={styles.note}>{setupMessage}</Text>
        </View>

        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Choose your bird</Text>
          <Text style={styles.counter}>4 BIRDS</Text>
        </View>

        <View style={styles.birdGrid}>
          {birdSounds.map((bird) => (
            <TouchableOpacity
              key={bird.id}
              style={[
                styles.birdCard,
                selectedBird === bird.id && styles.selectedBirdCard,
              ]}
              onPress={() => selectBird(bird.id)}
              activeOpacity={0.8}
            >
              <Text style={styles.birdEmoji}>{bird.emoji}</Text>
              <Text style={styles.birdName}>{bird.name}</Text>
              <Text style={styles.birdDescription}>{bird.description}</Text>

              {selectedBird === bird.id && (
                <Text style={styles.availableLabel}>✓ SELECTED</Text>
              )}
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.footer}>MADE FOR THE LOVE OF BIRDS</Text>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#101710',
  },
  content: {
    paddingHorizontal: 22,
    paddingTop: 28,
    paddingBottom: 40,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  logo: {
    fontSize: 42,
  },
  title: {
    color: '#F3F6ED',
    fontSize: 28,
    fontWeight: '800',
    letterSpacing: -0.5,
  },
  subtitle: {
    color: '#A5BD91',
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 3,
    marginTop: 2,
  },
  tagline: {
    color: '#B7C5AE',
    fontSize: 16,
    marginTop: 16,
    marginBottom: 28,
  },
  setupCard: {
    backgroundColor: '#1B2B1E',
    borderColor: '#344C35',
    borderWidth: 1,
    borderRadius: 22,
    padding: 20,
    marginBottom: 32,
  },
  sectionTitle: {
    color: '#F3F6ED',
    fontSize: 20,
    fontWeight: '700',
  },
  cardDescription: {
    color: '#B7C5AE',
    fontSize: 14,
    lineHeight: 21,
    marginTop: 8,
  },
  setupButton: {
    backgroundColor: '#B8D99A',
    borderRadius: 13,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 20,
  },
  setupButtonText: {
    color: '#182416',
    fontSize: 15,
    fontWeight: '800',
  },
  note: {
    color: '#94A58C',
    fontSize: 11,
    marginTop: 12,
    lineHeight: 16,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  counter: {
    color: '#A5BD91',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
  },
  birdGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  birdCard: {
    width: '48%',
    flexGrow: 1,
    flexBasis: '42%',
    backgroundColor: '#1A211A',
    borderColor: '#303C30',
    borderWidth: 1,
    borderRadius: 18,
    padding: 16,
    minHeight: 165,
  },
  birdEmoji: {
    fontSize: 32,
    marginBottom: 12,
  },
  birdName: {
    color: '#F3F6ED',
    fontSize: 16,
    fontWeight: '700',
  },
  birdDescription: {
    color: '#A8B5A0',
    fontSize: 12,
    marginTop: 6,
  },
  availableLabel: {
    color: '#B8D99A',
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 1,
    marginTop: 12,
  },
  footer: {
    color: '#71816A',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 2,
    textAlign: 'center',
    marginTop: 32,
  },
  selectedBirdCard: {
    backgroundColor: '#263A26',
    borderColor: '#B8D99A',
    borderWidth: 2,
  },
});
