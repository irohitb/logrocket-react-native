const {
  withProjectBuildGradle,
  createRunOncePlugin,
} = require('@expo/config-plugins');

// Because we need the package to be added AFTER the React and Google maven packages, we create a
// new allprojects. It's ok to have multiple allprojects.repositories, so we create a new one since
// it's cheaper than tokenizing the existing block to find the correct place to insert.
const gradleMaven =
  'allprojects { repositories { maven { url "https://storage.googleapis.com/logrocket-maven/" } } }';

function setGradleMaven(buildGradle) {
  if (buildGradle.includes('logrocket-maven')) {
    return buildGradle;
  }

  return `${buildGradle}\n${gradleMaven}\n`;
}

const withAndroidLogRocketMaven = config => {
  return withProjectBuildGradle(config, gradleConfig => {
    if (gradleConfig.modResults.language === 'groovy') {
      // eslint-disable-next-line no-param-reassign
      gradleConfig.modResults.contents = setGradleMaven(gradleConfig.modResults.contents);
    } else {
      throw new Error(
        'Cannot add LogRocket maven respository because the build.gradle is not groovy'
      );
    }

    return gradleConfig;
  });
};

const withLogRocket = config => {
  return withAndroidLogRocketMaven(config);
};

module.exports = createRunOncePlugin(withLogRocket, 'logrocket');
