const axios = require('axios');

async function testRateLimit() {
  const url = 'http://localhost:3000/health';
  let successCount = 0;
  let blockedCount = 0;

  console.log('Starting rate limit test...');

  for (let i = 0; i < 70; i++) {
    try {
      await axios.get(url);
      successCount++;
      process.stdout.write('.');
    } catch (error) {
      if (error.response && error.response.status === 429) {
        blockedCount++;
        process.stdout.write('x');
      } else {
        console.error('Unexpected error:', error.message);
      }
    }
  }

  console.log('\n\nResults:');
  console.log(`Successful requests: ${successCount}`);
  console.log(`Blocked requests: ${blockedCount}`);

  if (successCount <= 60 && blockedCount > 0) {
    console.log('✅ Rate limiting is WORKING');
  } else {
    console.log('❌ Rate limiting is NOT working as expected');
  }
}

testRateLimit();
