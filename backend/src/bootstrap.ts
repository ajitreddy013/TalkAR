import dns from 'dns';

// Force IPv4 to avoid ENETUNREACH on IPv6 in some environments (like Render/Supabase)
// This must run before any network connections are made
if (dns.setDefaultResultOrder) {
  dns.setDefaultResultOrder('ipv4first');
  console.log('Network: DNS resolution forced to IPv4 first');
}
