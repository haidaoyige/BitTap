"""Independent recovery of public known-scalar CG3, CG2 and pre-0.4 CG1 fixtures."""
import base64,hashlib,pathlib
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.serialization import Encoding,PublicFormat
from cryptography.exceptions import InvalidTag
root=pathlib.Path(__file__).resolve().parents[1]
expected=(root/'core/build/address-fixtures.tsv').read_text().splitlines()[0].split('\t')
for path,version in [(root/'core/build/interop-fixture.txt',2),(root/'core/src/test/resources/cg1-scalar-one.txt',1),(root/'core/build/cg3-scalar-one.txt',3)]:
 text=path.read_text().strip()
 assert text.startswith(f'CG{version}:')
 data=base64.urlsafe_b64decode(text[4:]+'='*((-len(text[4:]))%4))
 assert data[:10]==b'CGF'+bytes([version,0,1,1,15,8,1])
 salt=data[10:26];nonce=data[26:38]
 if version==1:
  assert data[38]==42
  claims=[data[39:81].decode()];end=81
 else:
  assert data[38]==4
  claims=[];end=39
  for type_id in range(1,5):
   assert data[end]==type_id
   length=data[end+1];claims.append(data[end+2:end+2+length].decode());end+=2+length
  assert claims==[expected[3],expected[1],expected[2],expected[4]]
 if version==3:
  length=data[end];note=data[end+1:end+1+length].decode('utf-8');assert note=='03 - 長期储蓄';end+=1+length
 aad=data[:end];cipher=data[end:];assert len(cipher)==48
 key=hashlib.scrypt(b'test fixture password only',salt=salt,n=32768,r=8,p=1,dklen=32,maxmem=128*1024*1024)
 private=AESGCM(key).decrypt(nonce,cipher,aad)
 assert private==bytes(31)+b'\x01'
 pub=ec.derive_private_key(int.from_bytes(private,'big'),ec.SECP256K1()).public_key().public_bytes(Encoding.X962,PublicFormat.CompressedPoint)
 assert pub.hex()=='0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798'
 assert claims[0]=='bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
 # Check every byte in the public address section is authenticated by GCM.
 for i in range(38,end):
  changed=bytearray(aad);changed[i]^=1
  try:AESGCM(key).decrypt(nonce,cipher,bytes(changed))
  except InvalidTag:pass
  else:raise AssertionError('Tampered public metadata accepted')
 try:AESGCM(key).decrypt(nonce,cipher[:-1]+bytes([cipher[-1]^1]),aad)
 except InvalidTag:pass
 else:raise AssertionError('Tampered ciphertext accepted')
report='Independent Python recovery PASS: CG3, CG2 and legacy CG1 fixtures decrypted to published scalar 1; compressed public key and addresses match; all public-address and wallet-note metadata bytes and ciphertext are authenticated.\n'
(root/'docs/INTEROP-RESULT.txt').write_text(report)
print(report.strip())
