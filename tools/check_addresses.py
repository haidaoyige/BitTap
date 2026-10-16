"""Independent reference check of public test scalars only; never reads user secrets."""
import hashlib
from pathlib import Path
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat

ROOT=Path(__file__).resolve().parents[1]
FIELD=0xfffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f
ORDER=0xfffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141
ALPHABET='123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz'
CHARSET='qpzry9x8gf2tvdw0s3jn54khce6mua7l'
def public(k):
    return ec.derive_private_key(k,ec.SECP256K1()).public_key().public_bytes(Encoding.X962,PublicFormat.CompressedPoint)
def point(k):
    n=ec.derive_private_key(k,ec.SECP256K1()).public_key().public_numbers()
    return n.x,n.y
def add(a,b):
    if a is None:return b
    if b is None:return a
    x,y=a;u,v=b
    if x==u and (y+v)%FIELD==0:return None
    slope=((3*x*x)*pow(2*y,-1,FIELD) if a==b else (v-y)*pow(u-x,-1,FIELD))%FIELD
    nx=(slope*slope-x-u)%FIELD
    return nx,(slope*(x-nx)-y)%FIELD
def hash160(b):return hashlib.new('ripemd160',hashlib.sha256(b).digest()).digest()
def b58check(version,payload):
    raw=bytes([version])+payload
    raw+=hashlib.sha256(hashlib.sha256(raw).digest()).digest()[:4]
    n=int.from_bytes(raw,'big');out=''
    while n:n,r=divmod(n,58);out=ALPHABET[r]+out
    return '1'*(len(raw)-len(raw.lstrip(b'\x00')))+out
def witness(version,program):
    bits=''.join(f'{byte:08b}' for byte in program)
    bits+='0'*((-len(bits))%5)
    data=[version]+[int(bits[i:i+5],2) for i in range(0,len(bits),5)]
    hrp='bc';values=[ord(c)>>5 for c in hrp]+[0]+[ord(c)&31 for c in hrp]+data+[0]*6
    chk=1;gen=[0x3b6a57b2,0x26508e6d,0x1ea119fa,0x3d4233dd,0x2a1462b3]
    for value in values:
        high=chk>>25;chk=((chk&0x1ffffff)<<5)^value
        for j in range(5):
            if (high>>j)&1:chk^=gen[j]
    chk^=1 if version==0 else 0x2bc830a3
    return hrp+'1'+''.join(CHARSET[x] for x in data+[(chk>>(5*(5-i)))&31 for i in range(6)])

count=0
for row in (ROOT/'core/build/address-fixtures.tsv').read_text().splitlines():
    raw,legacy,nested,native,taproot=row.split('\t');k=int(raw,16)
    pub=public(k);pkh=hash160(pub)
    assert b58check(0,pkh)==legacy
    assert b58check(5,hash160(b'\x00\x14'+pkh))==nested
    assert witness(0,pkh)==native
    x,y=point(k);even=(x,y if y%2==0 else FIELD-y)
    tag=hashlib.sha256(b'TapTweak').digest()
    tweak=int.from_bytes(hashlib.sha256(tag+tag+x.to_bytes(32,'big')).digest(),'big')
    assert tweak<ORDER
    output=add(even,point(tweak) if tweak else None)
    assert output is not None
    assert witness(1,output[0].to_bytes(32,'big'))==taproot
    # Verify the original scalar recovers the spending scalar for this key-only output.
    spend=((k if y%2==0 else ORDER-k)+tweak)%ORDER
    assert spend!=0 and point(spend)==output
    count+=1
report=f'Independent Python address verification PASS: {count} public scalar fixtures; P2PKH, P2SH-P2WPKH, P2WPKH and BIP341 key-only P2TR match; original scalars recover matching Taproot spending keys. No user keys read.\n'
(ROOT/'docs/ADDRESS-INTEROP-RESULT.txt').write_text(report)
print(report.strip())
