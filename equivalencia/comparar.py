"""
Resume equivalencia_android.csv (generado por EquivalenciaTest en el teléfono) para la tesis.
Uso: python comparar.py equivalencia_android.csv
"""
import csv, sys, math
R = list(csv.DictReader(open(sys.argv[1] if len(sys.argv) > 1 else 'equivalencia_android.csv', encoding='utf-8')))
f = lambda x: float(x) if x not in ('', 'NaN', 'nan') else math.nan
n = len(R); iguales = sum(r['coincide'] == 'true' for r in R)
dp = [f(r['dif_max_prob']) for r in R]; de = [f(r['dif_max_entrada']) for r in R]; dm = [f(r['dif_media_entrada']) for r in R]
ms = sorted(f(r['ms_analisis']) for r in R)
print(f'Imágenes procesadas: {n}')
print(f'Resultados iguales: {iguales} de {n}')
print(f'Diferencia máxima de probabilidad: {max(dp):.6f}  (media {sum(dp)/n:.6f})')
print(f'Diferencia máxima de un valor de entrada (0-255): {max(de):.4f}  (media de las medias {sum(dm)/n:.4f})')
print(f'Imágenes dentro de la tolerancia de 0,01: {sum(d <= 0.01 for d in dp)} de {n}')
for r in R:
    if r['coincide'] != 'true' or f(r['dif_max_prob']) > 0.01:
        print('  Diferencia:', r['archivo'], r['resultado_android'], r['resultado_python'], r['dif_max_prob'])
